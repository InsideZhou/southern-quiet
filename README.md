### 需要注意的局限
1. 因为Logging的特殊性，Logger的初始化在ApplicationContext之前，所以Logger的相关配置需要注意遵守Sprint Boot的[约定](http://docs.spring.io/spring-boot/docs/1.5.3.RELEASE/reference/htmlsingle/#boot-features-logging)。
1. 为了序列化结果的泛用性，不使用JAVA自带的二进制序列化。凡是涉及序列化的功能，如往Cache、Session中保存JAVA对象时，除非另有说明，否则该对象必须能正确被Jackson库进行JSON序列化。

### 编码及设计风格
1. 在文件系统上的目录，如果是框架自动创建的，应该统一用大写路径名，保持适当的特征有助于应用维护时大致区分某个目录来自何处。
