package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tender_pricings")
public class TenderPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Tender tender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingStatus status = PricingStatus.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedPricingManager;

    private BigDecimal estimatedTotal;
    private BigDecimal finalTotal;

    @Column(nullable = false)
    private String currency = "MAD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private User validatedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
