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
@Table(name = "tender_company_participations")
public class TenderCompanyParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    private PartnerCompany partnerCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationType participationType;

    @Column(nullable = false)
    private boolean isOurCompany;

    @Column(nullable = false)
    private boolean leadCompany;

    private BigDecimal sharePercentage;
    private String roleDescription;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
