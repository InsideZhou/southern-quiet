### 提供的特性
1. FileSystem 文件系统
    > 屏蔽底层细节，提供文件读写的支持，并且由于驱动的不同，可以做到不同机器上的应用共享同一个文件系统。

    - 考虑文件名的规范，在某些驱动上合法的文件名，在其他驱动上未必兼容。需要跨驱动的应用，特别需要注意。
    - 默认的驱动是本地文件系统，限制了文件名只能是数字、字母、下划线。查看FileSystemHelper类。

1. Cache 缓存
    - 默认驱动基于FileSystem，所以会有缓存KEY规范的问题需要考虑。
    - 默认驱动未处理缓存KEY。

### 需要注意的局限
1. 因为Logging的特殊性，Logger的初始化在ApplicationContext之前，所以Logger的相关配置需要注意遵守Sprint Boot的[约定]。
(http://docs.spring.io/spring-boot/docs/1.5.3.RELEASE/reference/htmlsingle/#boot-features-logging)

### 编码及设计风格
1. 在文件系统上的目录，如果是框架自动创建的，应该统一用大写路径名，保持适当的特征有助于应用维护时大致区分某个目录来自何处。
