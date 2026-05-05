package com.thomson.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.booking-queue}")
    private String bookingQueue;

    @Value("${rabbitmq.payment-queue}")
    private String paymentQueue;

    @Value("${rabbitmq.booking-routing-key}")
    private String bookingRoutingKey;

    @Value("${rabbitmq.payment-routing-key}")
    private String paymentRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue bookingNotificationQueue() {
        return new Queue(bookingQueue, true);
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return new Queue(paymentQueue, true);
    }

    @Bean
    public Binding bookingBinding() {
        return BindingBuilder.bind(bookingNotificationQueue()).to(exchange()).with(bookingRoutingKey);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentNotificationQueue()).to(exchange()).with(paymentRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
