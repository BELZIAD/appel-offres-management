package com.aomanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SettingsDtos {
    public record CompanySettingsResponse(
            Long id,
            String companyName,
            String applicationName,
            String companyLogo,
            String companyLogoDark,
            String favicon,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String dangerColor,
            String warningColor,
            String successColor,
            String sidebarColor,
            String headerColor,
            String address,
            String city,
            String postalCode,
            String country,
            String phone,
            String mobile,
            String email,
            String website,
            String ice,
            String ifNumber,
            String rc,
            String patente,
            String taxIdentifier,
            String currency,
            String timezone,
            String language,
            String linkedin,
            String facebook,
            String instagram,
            String youtube,
            LocalDateTime updatedAt
    ) {}

    public record CompanySettingsRequest(
            String companyName,
            String applicationName,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String dangerColor,
            String warningColor,
            String successColor,
            String sidebarColor,
            String headerColor,
            String address,
            String city,
            String postalCode,
            String country,
            String phone,
            String mobile,
            String email,
            String website,
            String ice,
            String ifNumber,
            String rc,
            String patente,
            String taxIdentifier,
            String currency,
            String timezone,
            String language,
            String linkedin,
            String facebook,
            String instagram,
            String youtube
    ) {}

    public record UserProfileRequest(String firstName, String lastName, String jobTitle, String department, String phone, String language, String timezone) {}
    public record UserPreferencesRequest(String theme, String language, boolean emailNotifications, boolean browserNotifications, String timezone, String dateFormat, String timeFormat, int pageSize) {}
    public record UserPreferencesResponse(String theme, String language, boolean emailNotifications, boolean browserNotifications, String timezone, String dateFormat, String timeFormat, int pageSize) {}
    public record ProfileUpdateRequest(UserProfileRequest profile, UserPreferencesRequest preferences) {}
    public record ProfileResponse(Long id, String firstName, String lastName, String email, String role, String photo, String signature, String jobTitle, String department, String phone, String language, String timezone, UserPreferencesResponse preferences) {}
    public record AboutResponse(CompanySettingsResponse company) {}
    public record BackupItem(String name, long size, LocalDateTime createdAt, String status, String checksumSha256) {}
    public record BackupListResponse(List<BackupItem> backups) {}
    public record UploadResponse(String url) {}
}
