package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "application_versions")
public class ApplicationVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String version;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime buildDate;
    private String commitGit;
    @Column(nullable = false) private String environment = "DEV";
}
