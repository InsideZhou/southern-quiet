package com.ai.southernquiet.job.driver;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * 这个接口存在的意义在于让jdk proxy生效，通过RabbitMQ的检查。
 *
 * @see org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor#checkProxy(Method, Object)
 */
public interface AmqpJobListener {
    @Transactional
    @RabbitListener(queues = "#{amqpJobProperties.workingQueue}")
    void process(Message message) throws Exception;
}
