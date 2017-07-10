### 必须知道的细节
1. 版本控制中忽略了application-default.properties文件，意味着开发者本地调试时，可以往该文件中写入配置，覆盖一些值来配合本地开发。

1. 每个模块都有与之对应的*AutoConfiguration类，模块依赖的外部配置使用*AutoConfiguration.*Properties来管理。

### 提供的特性
1. FileSystem 文件系统
    > 屏蔽底层细节，提供文件读写的支持，并且由于驱动的不同，可以做到不同机器上的应用共享同一个文件系统。

    - 考虑文件名的规范，在某些驱动上合法的文件名，在其他驱动上未必合法。需要跨驱动的应用，特别需要注意兼容性。查看FileSystemHelper.assertFileNameValid。

1. Session 会话
    > 基于FileSystem扩展Jetty的SessionDataStore，意味着对使用方透明，并且可以做到多应用会话共享。

1. Cache 缓存
    - 默认驱动基于FileSystem，所以会有缓存KEY规范的问题需要考虑。
    - 默认驱动未处理缓存KEY。

1. Logging 日志
    - 由于Logger初始化时间的原因，基于FileSystem的FileAppender在FileSystem未能初始化时，默认不输出日志。
    
1. Auth 身份及权限验证（可选）
    - 提供Auth注解来验证身份及权限，使用在Controller及Action上。  
    - 要打开这个特性，需要提供AuthService类型的Bean，并向SpringMVC注册AuthInterceptor。
    
1. JobQueue 任务队列
    - 继承Job完成任务类。
    - 获取JobScheduler Bean，将Job添加至调度器。
    - 可以自定义JobQueue实现来决定任务的处理顺序。
    - 任务被调度器执行回调时，可以将postpone设置为true来让调度器忽略本次回调，让该任务重新排队。
    - 要启用这个特性，添加EnableScheduling注解。


### 如何使用
1. 在应用入口类上使用SpringBootApplication或者EnableAutoConfiguration注解。

1. 向Spring的注解扫描添加com.ai.southernquiet包


### 需要注意的局限
1. 因为Logging的特殊性，Logger的初始化在ApplicationContext之前，所以Logger的相关配置需要注意遵守Sprint Boot的[约定]。
(http://docs.spring.io/spring-boot/docs/1.5.3.RELEASE/reference/htmlsingle/#boot-features-logging)

1. 启用spring-boot-devtools的时候，可能会因为同一个类被不同ClassLoader载入导致类型判断失败。


### 编码及设计风格
1. 在文件系统上的目录，如果是框架自动创建的，应该统一用大写路径名，保持适当的特征有助于应用维护时大致区分某个目录来自何处。
1. 遇到bug，应该优先考虑设计一个或一组测试来捕获这个bug，以提高代码的质量及可维护性。