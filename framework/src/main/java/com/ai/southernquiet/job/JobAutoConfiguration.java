package com.ai.southernquiet.job;

import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.job.driver.FileSystemJobQueue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties
public class JobAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(JobScheduler.class)
    public JobScheduler jobScheduler(@Qualifier(DefaultJobQueueCondition.QUALIFIER) JobQueue queue,
                                     @Qualifier(RetryJobQueueCondition.QUALIFIER) JobQueue retryQueue,
                                     ApplicationContext context,
                                     FrameworkProperties properties) {
        return new JobScheduler(queue, retryQueue, context, properties);
    }

    @Bean
    @Qualifier(DefaultJobQueueCondition.QUALIFIER)
    @Conditional(DefaultJobQueueCondition.class)
    public JobQueue jobQueue(FileSystem fileSystem, FrameworkProperties properties) {
        return new FileSystemJobQueue(fileSystem, properties);
    }

    @Bean
    @Qualifier(RetryJobQueueCondition.QUALIFIER)
    @Conditional(RetryJobQueueCondition.class)
    public JobQueue retryJobQueue(FileSystem fileSystem, FrameworkProperties properties) {
        FileSystemJobQueue queue = new FileSystemJobQueue(fileSystem, properties);
        queue.setWorkingRoot(queue.getWorkingRoot() + FileSystem.PATH_SEPARATOR + "RETRY");
        return queue;
    }

    public static class DefaultJobQueueCondition implements Condition {
        public final static String QUALIFIER = "default";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

            return Arrays.stream(context.getBeanFactory().getBeanNamesForType(JobScheduler.class))
                .map(name -> {
                    try {
                        return beanFactory.getBean(name).getClass();
                    }
                    catch (BeansException e) {
                        return null;
                    }
                })
                .filter(cls -> null != cls)
                .allMatch(cls -> {
                    Qualifier qualifier = cls.getAnnotation(Qualifier.class);
                    return null == qualifier || !qualifier.value().equals(DefaultJobQueueCondition.QUALIFIER);
                });
        }
    }

    public static class RetryJobQueueCondition implements Condition {
        public final static String QUALIFIER = "retry";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

            return Arrays.stream(context.getBeanFactory().getBeanNamesForType(JobScheduler.class))
                .map(name -> {
                    try {
                        return beanFactory.getBean(name).getClass();
                    }
                    catch (BeansException e) {
                        return null;
                    }
                })
                .filter(cls -> null != cls)
                .allMatch(cls -> {
                    Qualifier qualifier = cls.getAnnotation(Qualifier.class);
                    return null == qualifier || !qualifier.value().equals(RetryJobQueueCondition.QUALIFIER);
                });
        }
    }
}
