package com.thomson.bookingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Check(constraints = "status IN ('PENDING', 'CONFIRMED', 'CANCELLED')")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long flightId;
    private String seatNumber;
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime bookingTime;
    private LocalDateTime expiresAt;
}
