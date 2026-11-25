package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tenders")
public class Tender {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    private String marketNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String organization;

    private LocalDate publicationDate;

    @Column(nullable = false)
    private LocalDate deadline;

    private BigDecimal estimatedAmount;

    private BigDecimal provisionalGuarantee;

    private BigDecimal finalAmount;

    @Column(columnDefinition = "TEXT")
    private String lossReason;

    private LocalDate resultDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderStatus status = TenderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderResult result = TenderResult.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationMode participationMode = ParticipationMode.SOLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private User assignedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
