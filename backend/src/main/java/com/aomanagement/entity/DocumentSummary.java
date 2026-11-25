package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_summaries")
public class DocumentSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private DocumentAnalysis analysis;

    @Column(columnDefinition = "TEXT")
    private String shortSummary;

    @Column(columnDefinition = "TEXT")
    private String keyPoints;

    @Column(columnDefinition = "TEXT")
    private String importantDates;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(columnDefinition = "TEXT")
    private String financialConditions;

    @Column(columnDefinition = "TEXT")
    private String technicalConditions;

    @Column(columnDefinition = "TEXT")
    private String attentionPoints;
}
