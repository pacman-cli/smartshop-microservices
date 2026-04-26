package com.smartshop.payment.repository;

import com.smartshop.payment.entity.Payment;
import com.smartshop.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderNumber(String orderNumber);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
