/*
 * Copyright 2013 The Netty Project
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

package io.netty.buffer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.netty.util.internal.ReferenceCountUpdater;

/**
 * 引用计数，用于跟踪对象的分配和销毁，做自动内存回收
 * UnpooledDirectByteBuf、UnpooledHeapByteBuf等的抽象父类
 *
 * 在Netty中，经常使用的ByteBuf的实现类如UnpooledHeapByteBuf、UnpooledDirectByteBuf、pooledHeapByteBuf、pooledDirectByteBuf等都继承自AbstractReferenceCountedByteBuf类。
 * 这个类的主要功能是对引用进行计数。ByteBuf通过对引用计数，可以知道自己被引用的次数，便于池化的ByteBuf或DirectByteBuf等进行内存回收和释放。
 * （注意：非池化的ByteBuf每次I/O都会创建一个ByteBuf，可由JVM管理其生命周期，但UnpooledDirectByteBuf最好也手动释放；池化的ByteBuf要手动进行内存回收和释放。）
 * Abstract base class for {@link ByteBuf} implementations that count references.
 */
public abstract class AbstractReferenceCountedByteBuf extends AbstractByteBuf {

    /**
     * ReferenceCountUpdater原子更新封装类,
     * 作为抽象类暴露了两个重要的方法：updater()、unsafeOffset()
     * REFCNT_FIELD_OFFSET表示refCnt字段在AbstractReferenceCountedByteBuf中的内存地址。
     *
     * 此处的内存地址是通过JDK的unsafe方法来获取objectFieldOffset的，在ByteBuf的子类：
     * UnpooledUnsafeDirectByteBuf和PooledUnsafeDirectByteBuf会使用这个内存地址（偏移量）
     *
     */
    private static final long REFCNT_FIELD_OFFSET =
            ReferenceCountUpdater.getUnsafeOffset(AbstractReferenceCountedByteBuf.class, "refCnt");
    /**
     * 新new出一个AtomicIntegerFieldUpdater对象，用于updater对象获取AtomicIntegerFieldUpdater
     * 1. AtomicIntegerFieldUpdater通过原子的方式对成员变量进行更新等操作，主要为了实现线程安全，消除锁
     * 2. 为什么不直接用AtomicInteger来实现原子操作呢？这是因为：AtomicInteger类型创建的对象比int类型多占用16B的对象头，在
     *  Netty这类高并发程序中，当有几十万或者几百万ByteBuf对象时，节约的内存可能就是几十MB或几百MB，节约的内存数还是非常可观的。
     */
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> AIF_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");

    private static final ReferenceCountUpdater<AbstractReferenceCountedByteBuf> updater =
            new ReferenceCountUpdater<AbstractReferenceCountedByteBuf>() {
        @Override
        protected AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> updater() {
            return AIF_UPDATER;
        }
        @Override
        protected long unsafeOffset() {
            return REFCNT_FIELD_OFFSET;
        }
    };

    /**
     * Value might not equal "real" reference count, all access should be via the updater
     * 此处定义的refCnt字段用于追踪对象的引用次数，使用volatile定义为了解决多线程并发访问的可见性问题
     */
    @SuppressWarnings("unused")
    private volatile int refCnt = updater.initialValue();

    protected AbstractReferenceCountedByteBuf(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    boolean isAccessible() {
        // Try to do non-volatile read for performance as the ensureAccessible() is racy anyway and only provide
        // a best-effort guard.
        return updater.isLiveNonVolatile(this);
    }

    @Override
    public int refCnt() {
        return updater.refCnt(this);
    }

    /**
     * An unsafe operation intended for use by a subclass that sets the reference count of the buffer directly
     */
    protected final void setRefCnt(int refCnt) {
        updater.setRefCnt(this, refCnt);
    }

    /**
     * An unsafe operation intended for use by a subclass that resets the reference count of the buffer to 1
     */
    protected final void resetRefCnt() {
        updater.resetRefCnt(this);
    }

    /**
     * AbstractReferenceCountedByteBuf委托updater去对refCnt进行增加计数
     * @return
     */
    @Override
    public ByteBuf retain() {
        return updater.retain(this);
    }

    @Override
    public ByteBuf retain(int increment) {
        return updater.retain(this, increment);
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return handleRelease(updater.release(this));
    }

    @Override
    public boolean release(int decrement) {
        return handleRelease(updater.release(this, decrement));
    }

    private boolean handleRelease(boolean result) {
        if (result) {
            deallocate();
        }
        return result;
    }

    /**
     * Called once {@link #refCnt()} is equals 0.
     */
    protected abstract void deallocate();
}
