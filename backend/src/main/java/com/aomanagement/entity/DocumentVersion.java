package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "document_versions", uniqueConstraints = @UniqueConstraint(name = "uk_document_versions_document_version", columnNames = {"document_id", "version_number"}))
public class DocumentVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private TenderDocument document;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, length = 1000)
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
