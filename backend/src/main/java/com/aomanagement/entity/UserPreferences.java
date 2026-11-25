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
@Table(name = "user_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_user_preferences_user", columnNames = "user_id"))
public class UserPreferences {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String theme = "SYSTEM";
    @Column(nullable = false) private String language = "fr";
    @Column(nullable = false) private boolean emailNotifications = true;
    @Column(nullable = false) private boolean browserNotifications = true;
    @Column(nullable = false) private String timezone = "Africa/Casablanca";
    @Column(nullable = false) private String dateFormat = "dd/MM/yyyy";
    @Column(nullable = false) private String timeFormat = "HH:mm";
    @Column(nullable = false) private int pageSize = 20;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
}
