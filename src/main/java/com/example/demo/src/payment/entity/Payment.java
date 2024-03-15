package com.example.demo.src.payment.entity;

import com.example.demo.common.entity.BaseEntity;
import com.example.demo.src.subscription.entity.Subscription;
import com.example.demo.src.user.entity.User;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static javax.persistence.FetchType.LAZY;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@JsonAutoDetect(fieldVisibility = ANY)
@Table(name = "TB_PAYMENT")
public class Payment extends BaseEntity {

    @Id
    @Column(name = "paymentId", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "subscriptionId")
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 10)
    private State state;

    public enum State {
        SUCCESS, FAIL;
    }

}
