package com.thomson.bookingservice.repository;

import com.thomson.bookingservice.entity.Booking;
import com.thomson.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
}
