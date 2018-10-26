package com.ai.southernquiet.job.driver;

import com.ai.southernquiet.job.JobEngine;
import org.springframework.amqp.core.Message;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 这个接口存在的意义在于让jdk proxy生效，通过RabbitMQ的检查。
 *
 * @see org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor#checkProxy(Method, Object)
 */
public interface AmqpJobEngine<T extends Serializable> extends JobEngine<T> {
    void process(Message message) throws Exception;
}
