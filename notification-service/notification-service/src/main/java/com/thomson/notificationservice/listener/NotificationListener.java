package com.thomson.notificationservice.listener;

import com.thomson.notificationservice.dto.BookingEvent;
import com.thomson.notificationservice.dto.PaymentEvent;
import com.thomson.notificationservice.service.EmailService;
import com.thomson.notificationservice.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;
    private final EmailTemplateService templateService;

    @Value("${notification.from-email}")
    private String defaultEmail;

    @RabbitListener(queues = "${rabbitmq.booking-queue}")
    public void handleBookingEvent(BookingEvent event) {
        log.info("Received booking event: {} for booking {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "BOOKING_CONFIRMED" -> emailService.send(
                    resolveEmail(event.getUserId()),
                    "Booking Confirmed — AirSync #" + event.getBookingId(),
                    templateService.bookingConfirmed(event.getBookingId(), event.getSeatNumber(), event.getTotalPrice())
            );
            case "BOOKING_CANCELLED" -> emailService.send(
                    resolveEmail(event.getUserId()),
                    "Booking Cancelled — AirSync #" + event.getBookingId(),
                    templateService.bookingCancelled(event.getBookingId(), event.getSeatNumber())
            );
            default -> log.warn("Unknown booking event type: {}", event.getEventType());
        }
    }

    @RabbitListener(queues = "${rabbitmq.payment-queue}")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for booking {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "PAYMENT_SUCCESS" -> emailService.send(
                    event.getEmail(),
                    "Payment Receipt — AirSync #" + event.getBookingId(),
                    templateService.paymentSuccess(event.getBookingId(), event.getAmount(), event.getPaymentId())
            );
            case "PAYMENT_FAILED" -> emailService.send(
                    event.getEmail(),
                    "Payment Failed — AirSync #" + event.getBookingId(),
                    templateService.paymentFailed(event.getBookingId(), event.getAmount())
            );
            default -> log.warn("Unknown payment event type: {}", event.getEventType());
        }
    }

    // TODO: replace with a Feign call to user-service GET /api/users/{id}/email
    private String resolveEmail(Long userId) {
        return defaultEmail;
    }
}
