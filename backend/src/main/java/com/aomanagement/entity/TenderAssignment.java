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
@Table(name = "tender_assignments", uniqueConstraints = @UniqueConstraint(name = "uk_tender_assignment_role_active", columnNames = {"tender_id", "employee_id", "assignment_role", "active"}))
public class TenderAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderAssignmentRole assignmentRole;

    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
