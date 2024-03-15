package com.example.demo.src.payment;

import com.example.demo.common.entity.BaseEntity;
import com.example.demo.src.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import static com.example.demo.common.entity.BaseEntity.*;
import static com.example.demo.src.payment.entity.Payment.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByImpUidAndState(String impUid, State state);
    Optional<Payment> findByMerchantUidAndState(String merchantUid, State state);

    List<Payment> findAllByPaymentState(PaymentState paymentState);
}
