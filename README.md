### 必须知道的细节
1. 版本控制中忽略了application-default.properties文件，意味着本地调试时，可以往该文件中写入配置，覆盖一些值来适应本地开发环境。

1. 每个模块都有与之对应的*AutoConfiguration类，模块依赖的外部配置使用*AutoConfiguration.*Properties来管理。

1. 每个*AutoConfiguration类都有与之对应的spring.factories，以便在AutoConfigure时自动载入。


### 提供的特性
##### FileSystem 文件系统  

> 屏蔽底层细节，提供文件读写的支持，并且由于驱动的不同，可以做到不同机器上的应用共享同一个文件系统。

- filesystem-spring-boot-starter-*
- 考虑文件名的规范，在某些驱动上合法的文件名，在其他驱动上未必合法。需要跨驱动的应用，特别需要注意兼容性。查看FileSystemSupport.assertFileNameValid。
- 考虑文件并发读写的问题。由于某些驱动在读文件时并不会独占文件，所以需要充分考虑事务的级别。

##### Session 会话  

- session-spring-boot-starter-*

##### Logging 日志

- logging-spring-boot-starter-*
- 由于Logger初始化时间的原因，基于FileSystem的FileAppender在FileSystem未能初始化时，默认不输出日志。
    
##### Auth 身份及权限验证

- 提供Auth注解来验证身份及权限，使用在Controller及Action上。  
- 要打开这个特性，需要提供AuthService类型的Bean。
    
##### KeyValueStore 键值对存储（默认framework.key-value.enable=false）

- 默认驱动基于FileSystem，会有KEY规范的问题需要考虑。
    
##### IdGenerator 发号器/Id生成器

- id-generator-spring-boot-starter-*
    
##### EventBroadcasting 事件广播 (At Most Once Message)

> 把基于Spring Event的自定义事件广播到当前ApplicationContext之外

- event-spring-boot-starter-*
- me.insidezhou.southernquiet.event.ShouldBroadcast
- me.insidezhou.southernquiet.event.CustomApplicationEvent
    
##### NotificationPublishing 通知发布 (At Least Once Message)

- notification-spring-boot-starter-*
    
##### JobEngine 任务引擎 (Exactly Once Message)

- job-spring-boot-starter-*

##### Throttle 节流器  

> 对某个数据进行截流一段时间或截流一定次数后，再重新打开

- framework自带默认的基于本地内存的时间节流器，只能用于单实例的应用。强烈不建议用于分布式应用。

- throttle-spring-boot-starter-redis 是使用redis实现的用于分布式应用的节流器。

###### throttle-spring-boot-starter-redis如何使用：
有两种节流器

1、基于时间的节流器RedisTimeBaseThrottle：open(String key,long threshold)，threshold为节流时间（毫秒）

2、基于计数器的节流器RedisCounterBaseThrottle：open(String key,long threshold)，threshold为节流次数（开闸后节流多少次）

如何获取节流器：

项目中引入throttle-spring-boot-starter-redis模块后，使用注入或ThrottleManager获取节流器，样例代码如下：
```java
    
@Controller
public class DemoController {

    @Autowired
    private ThrottleManager throttleManager;

    //注入redisTimeBaseThrottle
    @Resource(name = RedisThrottleBeanName.redisTimeBaseThrottle)
    private Throttle redisTimeBaseThrottle;

    //注入redisCounterBaseThrottle
    @Resource(name = RedisThrottleBeanName.redisCounterBaseThrottle)
    private Throttle redisCounterBaseThrottle;

    public void getThrottleExample() {
        //获取RedisTimeBaseThrottle
        Throttle redisTimeBaseThrottle = throttleManager.getThrottle();

        //获取RedisCounterBaseThrottle
        Throttle redisCounterBaseThrottle = throttleManager.getThrottle(RedisThrottleBeanName.redisCounterBaseThrottle);
    }
}

```

### 如何使用
1. 在应用入口类上使用SpringBootApplication或者EnableAutoConfiguration注解。


### 需要注意的局限
1. 因为Logging的特殊性，Logger的初始化在ApplicationContext之前，所以Logger的相关配置需要注意遵守Spring Boot的
[约定](https://docs.spring.io/spring-boot/docs/2.0.6.RELEASE/reference/htmlsingle/#boot-features-custom-log-configuration)。

1. 启用spring-boot-devtools的时候，可能会因为同一个类被不同ClassLoader载入导致类型判断失败。


### 编码及设计风格
1. 在文件系统上的目录，如果是框架使用默认值创建的，应该统一用大写路径名，保持适当的特征有助于应用维护时大致区分某个目录来自何处。
1. 遇到bug，应该优先考虑设计一个或一组测试来捕获这个bug，以提高代码的质量及可维护性。