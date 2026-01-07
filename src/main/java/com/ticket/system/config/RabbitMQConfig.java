package com.ticket.system.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    /** 正常退款交换机 */
    @Bean
    public DirectExchange refundExchange() {
        return new DirectExchange("refund.exchange", true, false);
    }

    /** 延迟交换机 */
    @Bean
    public DirectExchange refundDelayExchange() {
        return new DirectExchange("refund.delay.exchange", true, false);
    }

    /** 正常退款队列 */
    @Bean
    public Queue refundQueue() {
        return QueueBuilder
                .durable("refund.queue")
                .build();
    }

    /** 延迟退款队列（TTL + DLX） */
    @Bean
    public Queue refundDelayQueue() {
        return QueueBuilder
                .durable("refund.delay.queue")
                .withArgument("x-dead-letter-exchange", "refund.exchange")
                .withArgument("x-dead-letter-routing-key", "refund.key")
                .withArgument("x-message-ttl", 30_000) // 30秒后重试
                .build();
    }

    /** 正常队列绑定 */
    @Bean
    public Binding refundBinding() {
        return BindingBuilder
                .bind(refundQueue())
                .to(refundExchange())
                .with("refund.key");
    }

    /** 延迟队列绑定 */
    @Bean
    public Binding refundDelayBinding() {
        return BindingBuilder
                .bind(refundDelayQueue())
                .to(refundDelayExchange())
                .with("refund.delay.key");
    }

    // 订单交换机
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange", true, false);
    }

    // 订单队列
    @Bean
    public Queue orderQueue() {
        return new Queue("order.queue", true);
    }

    // 订单延迟队列
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "order.exchange");
        args.put("x-dead-letter-routing-key", "order.cancel");
        args.put("x-message-ttl", 1800000); // 30分钟
        return new Queue("order.delay.queue", true, false, false, args);
    }

    // 车票队列
    @Bean
    public Queue ticketQueue() {
        return new Queue("ticket.queue", true);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.create");
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderExchange())
                .with("order.delay");
    }



    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(1);
        return factory;
    }
}