package com.thomson.paymentservice.repository;

import com.thomson.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
}
