package com.thomson.bookingservice.publisher;

import com.thomson.bookingservice.dto.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public void publish(BookingEvent event) {
        log.info("Publishing event: {} for booking {}", event.getEventType(), event.getBookingId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
