package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "checklist_template_items")
public class ChecklistTemplateItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean required = true;

    @Enumerated(EnumType.STRING)
    private DocumentType expectedDocumentType;
}
