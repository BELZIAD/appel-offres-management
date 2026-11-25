package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "extracted_tender_information")
public class ExtractedTenderInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DocumentAnalysis analysis;

    @Column(nullable = false)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String fieldValue;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.ZERO;

    private Integer sourcePage;

    @Column(length = 500)
    private String sourceExcerpt;

    @Column(nullable = false)
    private boolean validated;

    @Column(nullable = false)
    private boolean ignored;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
