# netty-sourcecode-analysis
netty-sourcecode-analysis

## 问题修复

- 编译过程中，报：io.netty.util.collection包不存在解决方法：

1. cd common
2. mvn clean compile -Dcheckstyle.skip=true