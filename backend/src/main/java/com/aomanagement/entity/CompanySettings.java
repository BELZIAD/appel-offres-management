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
@Table(name = "company_settings")
public class CompanySettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column private String companyName;
    @Column(nullable = false) private String applicationName;
    private String companyLogo;
    private String companyLogoDark;
    private String favicon;
    @Column(nullable = false) private String primaryColor = "#172033";
    @Column(nullable = false) private String secondaryColor = "#475569";
    @Column(nullable = false) private String accentColor = "#2563eb";
    @Column(nullable = false) private String dangerColor = "#dc2626";
    @Column(nullable = false) private String warningColor = "#d97706";
    @Column(nullable = false) private String successColor = "#059669";
    @Column(nullable = false) private String sidebarColor = "#ffffff";
    @Column(nullable = false) private String headerColor = "#ffffff";
    @Column(columnDefinition = "TEXT") private String address;
    private String city;
    private String postalCode;
    private String country;
    private String phone;
    private String mobile;
    private String email;
    private String website;
    private String ice;
    @Column(name = "if_number") private String ifNumber;
    private String rc;
    private String patente;
    private String taxIdentifier;
    @Column(nullable = false) private String currency = "MAD";
    @Column(nullable = false) private String timezone = "Africa/Casablanca";
    @Column(nullable = false) private String language = "fr";
    private String linkedin;
    private String facebook;
    private String instagram;
    private String youtube;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
}
