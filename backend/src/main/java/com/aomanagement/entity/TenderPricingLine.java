package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tender_pricing_lines")
public class TenderPricingLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TenderPricing pricing;

    private String lineNumber;
    private String reference;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String designation;

    private String unit;

    @Column(nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private int position;

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal pu = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        totalPrice = q.multiply(pu).setScale(2, RoundingMode.HALF_UP);
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
