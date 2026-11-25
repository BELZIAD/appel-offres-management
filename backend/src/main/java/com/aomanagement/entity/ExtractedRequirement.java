package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "extracted_requirements")
public class ExtractedRequirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DocumentAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementCategory category = RequirementCategory.OTHER;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean required = true;

    private Integer sourcePage;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementStatus status = RequirementStatus.DETECTED;
}
