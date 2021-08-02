/*
 * Copyright 2019 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.internal;

import static io.netty.util.internal.ObjectUtil.checkPositive;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.netty.util.IllegalReferenceCountException;
import io.netty.util.ReferenceCounted;

/**
 * Common logic for {@link ReferenceCounted} implementations
 */
public abstract class ReferenceCountUpdater<T extends ReferenceCounted> {
    /*
     * Implementation notes:
     *
     * For the updated int field:
     *   Even => "real" refcount is (refCnt >>> 1)
     *   Odd  => "real" refcount is 0
     *
     * (x & y) appears to be surprisingly expensive relative to (x == y). Thus this class uses
     * a fast-path in some places for most common low values when checking for live (even) refcounts,
     * for example: if (rawCnt == 2 || rawCnt == 4 || (rawCnt & 1) == 0) { ...
     *
     * 此处注释说明了，如果是偶数，真实的引用值为 refCnt >>> 1 （除2）
     * 如果是奇数，则真实的引用值为0，表示待销毁。
     */

    protected ReferenceCountUpdater() { }

    public static long getUnsafeOffset(Class<? extends ReferenceCounted> clz, String fieldName) {
        try {
            if (PlatformDependent.hasUnsafe()) {
                return PlatformDependent.objectFieldOffset(clz.getDeclaredField(fieldName));
            }
        } catch (Throwable ignore) {
            // fall-back
        }
        return -1;
    }

    protected abstract AtomicIntegerFieldUpdater<T> updater();

    protected abstract long unsafeOffset();

    /**
     * TODO 这里初始值为啥是2？？
     *
     * refCnt是偶数表示当前缓冲区的状态为正常状态（有引用之后是累加2），如果
     * refCnt是奇数，则表示缓冲区的状态为待销毁状态。缓冲区引用寄数的真实值是refCnt / 2。
     *
     * @return
     */
    public final int initialValue() {
        return 2;
    }

    private static int realRefCnt(int rawCnt) {
        // rawCnt & 1 != 0 为true，即表示rawCnt为true
        return rawCnt != 2 && rawCnt != 4 && (rawCnt & 1) != 0 ? 0 : rawCnt >>> 1;
    }

    /**
     * Like {@link #realRefCnt(int)} but throws if refCnt == 0
     */
    private static int toLiveRealRefCnt(int rawCnt, int decrement) {
        if (rawCnt == 2 || rawCnt == 4 || (rawCnt & 1) == 0) {      // 当rawCnt为偶数时，真实引用值需要右移1位，？？？为啥要右移一位呢？
            return rawCnt >>> 1;
        }
        // odd rawCnt => already deallocated
        throw new IllegalReferenceCountException(0, -decrement);        // 奇数rawCnt表示已经释放，此时会抛出异常
    }

    private int nonVolatileRawCnt(T instance) {
        // TODO: Once we compile against later versions of Java we can replace the Unsafe usage here by varhandles.
        final long offset = unsafeOffset();         // 获取偏移量
        return offset != -1 ? PlatformDependent.getInt(instance, offset) : updater().get(instance); // 若偏移量正常，则选择Unsafe的普通get；若偏移量获取异常，则选择Unsafe的volatile get
    }

    public final int refCnt(T instance) {
        return realRefCnt(updater().get(instance));
    }

    public final boolean isLiveNonVolatile(T instance) {
        final long offset = unsafeOffset();
        final int rawCnt = offset != -1 ? PlatformDependent.getInt(instance, offset) : updater().get(instance);

        // The "real" ref count is > 0 if the rawCnt is even.
        return rawCnt == 2 || rawCnt == 4 || rawCnt == 6 || rawCnt == 8 || (rawCnt & 1) == 0;
    }

    /**
     * An unsafe operation that sets the reference count directly
     */
    public final void setRefCnt(T instance, int refCnt) {
        updater().set(instance, refCnt > 0 ? refCnt << 1 : 1); // overflow OK here
    }

    /**
     * Resets the reference count to 1
     */
    public final void resetRefCnt(T instance) {
        updater().set(instance, initialValue());
    }

    /**
     * retain：持有，保留
     *
     * 持有一个ByteBuf内存对象，增加引用计数器的引用
     *
     * @param instance
     * @return
     */
    public final T retain(T instance) {
        return retain0(instance, 1, 2);
    }

    public final T retain(T instance, int increment) {
        // all changes to the raw count are 2x the "real" change - overflow is OK
        int rawIncrement = checkPositive(increment, "increment") << 1;
        return retain0(instance, increment, rawIncrement);
    }

    /**
     *
     * rawIncrement == increment << 1  此处表示递增的值 1 << 1 等于 2
     *
     * @param instance
     * @param increment
     * @param rawIncrement
     * @return
     */
    private T retain0(T instance, final int increment, final int rawIncrement) {
        int oldRef = updater().getAndAdd(instance, rawIncrement);   // CAS 先获取旧的引用值，再增加2
        if (oldRef != 2 && oldRef != 4 && (oldRef & 1) != 0) {      // 如果旧的ref为奇数，则直接抛出异常       oldRef & 1 与运算，结果为奇数则不等于0，为偶数则等于0
            throw new IllegalReferenceCountException(0, increment);
        }
        // don't pass 0!
        if ((oldRef <= 0 && oldRef + rawIncrement >= 0)
                || (oldRef >= 0 && oldRef + rawIncrement < oldRef)) {
            // overflow case
            updater().getAndAdd(instance, -rawIncrement);
            throw new IllegalReferenceCountException(realRefCnt(oldRef), increment);
        }
        return instance;
    }

    /**
     * release()
     * 方法返回true表示无引用，此时会调用ByteBuf的deallocate()进行销毁
     * 方法返回false表示还有引用存在
     */
    public final boolean release(T instance) {
        int rawCnt = nonVolatileRawCnt(instance);           // 通过Unsafe的方式获取引用计数的值，先采用普通方法获取refCnt的值，无需采用volatile获取
        return rawCnt == 2 ? tryFinalRelease0(instance, 2) || retryRelease0(instance, 1)        // 如果rawCnt为2，表示还有引用存在
                : nonFinalRelease0(instance, 1, rawCnt, toLiveRealRefCnt(rawCnt, 1));
    }

    public final boolean release(T instance, int decrement) {
        int rawCnt = nonVolatileRawCnt(instance);
        int realCnt = toLiveRealRefCnt(rawCnt, checkPositive(decrement, "decrement"));
        return decrement == realCnt ? tryFinalRelease0(instance, rawCnt) || retryRelease0(instance, decrement)
                : nonFinalRelease0(instance, decrement, rawCnt, realCnt);
    }

    /**
     * 采用CAS最终释放，将refCnt设置为1
     */
    private boolean tryFinalRelease0(T instance, int expectRawCnt) {
        return updater().compareAndSet(instance, expectRawCnt, 1); // any odd number will work
    }

    /**
     * 非最后一次释放，realCnt > 1
     */
    private boolean nonFinalRelease0(T instance, int decrement, int rawCnt, int realCnt) {
        if (decrement < realCnt
                // all changes to the raw count are 2x the "real" change - overflow is OK
                && updater().compareAndSet(instance, rawCnt, rawCnt - (decrement << 1))) {
            return false;
        }
        return retryRelease0(instance, decrement);
    }

    private boolean retryRelease0(T instance, int decrement) {
        for (;;) {
            int rawCnt = updater().get(instance), realCnt = toLiveRealRefCnt(rawCnt, decrement);            // 获取实例instance中的rawCnt值，并计算出真实的realCnt
            if (decrement == realCnt) { // 如果要减少的引用值和真实的refCnt值相同，也即需要释放缓冲区对象，则调用tryFinalRelease0方法将refCnt的数值修改为1（只要修改为奇数即可）
                if (tryFinalRelease0(instance, rawCnt)) {       // 将refCnt数值修改为1
                    return true;
                }
            } else if (decrement < realCnt) {
                // all changes to the raw count are 2x the "real" change
                if (updater().compareAndSet(instance, rawCnt, rawCnt - (decrement << 1))) {
                    return false;
                }
            } else {
                throw new IllegalReferenceCountException(realCnt, -decrement);
            }
            Thread.yield(); // this benefits throughput under high contention    调用Thread.yield()方法释放出CPU的执行权，因为修改引用计数的逻辑在整个系统逻辑的优先级并不高，所以让出执行权有利于提高高并发下的系统吞吐量
        }
    }
}
