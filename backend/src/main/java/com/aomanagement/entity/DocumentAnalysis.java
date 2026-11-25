package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "document_analyses")
public class DocumentAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionMethod extractionMethod = ExtractionMethod.FAILED;

    @Column(nullable = false)
    private String detectedLanguage = "UNKNOWN";

    @Column(nullable = false)
    private int pageCount;

    @Column(nullable = false)
    private int textLength;

    @Column(nullable = false)
    private boolean requiresOcr;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String normalizedText;

    @Column(nullable = false)
    private String analysisEngineVersion = "RULES_V1";

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
