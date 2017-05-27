### 提供的特性
1. FileSystem 文件系统
    > 屏蔽底层细节，提供文件读写的支持，并且由于驱动的不同，可以做到不同机器上的应用共享同一个文件系统。

    - 考虑文件名的规范，在某些驱动上合法的文件名，在其他驱动上未必合法。需要跨驱动的应用，特别需要注意兼容性。
    - 默认的驱动是本地文件系统，限制了文件名只能是数字、字母、下划线。查看FileSystemHelper类。

1. Session 会话
    > 基于FileSystem扩展Jetty的SessionDataStore，意味着对使用方透明，并且可以做到多应用会话共享。

1. Cache 缓存
    - 默认驱动基于FileSystem，所以会有缓存KEY规范的问题需要考虑。
    - 默认驱动未处理缓存KEY。

1. Logging 日志
    - 由于Logger初始化时间的原因，基于FileSystem的FileAppender在FileSystem未能初始化时，默认不输出日志。
    
1. Auth 身份及权限验证
    > 提供Auth注解来验证身份及权限，使用在Controller及Action上。


### 如何使用
1. 向Spring的注解扫描添加com.ai.southernquiet包

1. 开启Spring的Type-Safe配置（@EnableConfigurationProperties）

1. 执行框架初始化
继承抽象类WebInit，并返回ServletContextInitializer类型的Bean，框架以此介入应用初始化过程。

1. 使用定制的Jetty容器逻辑
提供JettyServletWebServerFactory类型的Bean，该Bean需调用addConfigurations，添加自定义配置JettyConfiguration。

1. 开启注解身份验证及权限检查（可选）
提供AuthService类型的Bean，并向SpringMVC注册AuthInterceptor。


### 需要注意的局限
1. 因为Logging的特殊性，Logger的初始化在ApplicationContext之前，所以Logger的相关配置需要注意遵守Sprint Boot的[约定]。
(http://docs.spring.io/spring-boot/docs/1.5.3.RELEASE/reference/htmlsingle/#boot-features-logging)

1. build.gradle与buildscript.gradle有重复，是因为gradle当前路径计算的限制。


### 编码及设计风格
1. 在文件系统上的目录，如果是框架自动创建的，应该统一用大写路径名，保持适当的特征有助于应用维护时大致区分某个目录来自何处。
