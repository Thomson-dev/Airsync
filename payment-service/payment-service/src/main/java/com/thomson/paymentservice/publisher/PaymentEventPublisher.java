package com.thomson.paymentservice.publisher;

import com.thomson.paymentservice.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void publish(PaymentEvent event) {
        log.info("Publishing event: {} for payment {}", event.getEventType(), event.getPaymentId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
