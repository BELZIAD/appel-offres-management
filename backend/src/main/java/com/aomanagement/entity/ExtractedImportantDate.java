package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "extracted_important_dates")
public class ExtractedImportantDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DocumentAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false)
    private ImportantDateType type = ImportantDateType.OTHER;

    private LocalDate detectedDate;

    @Column(nullable = false)
    private String label;

    private Integer sourcePage;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence = BigDecimal.ZERO;
}
