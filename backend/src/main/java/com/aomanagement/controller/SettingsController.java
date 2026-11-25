package com.aomanagement.controller;

import com.aomanagement.dto.SettingsDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SettingsController {
    private final CompanySettingsService companySettingsService;
    private final ProfileService profileService;
    private final AssetStorageService assetStorageService;
    private final SystemInfoService systemInfoService;
    private final BackupService backupService;

    @GetMapping("/api/company-settings/public")
    public SettingsDtos.CompanySettingsResponse publicSettings() { return companySettingsService.current(); }

    @GetMapping("/api/admin/company")
    public SettingsDtos.CompanySettingsResponse company() { return companySettingsService.current(); }

    @PutMapping("/api/admin/company")
    public SettingsDtos.CompanySettingsResponse updateCompany(@RequestBody SettingsDtos.CompanySettingsRequest request, @AuthenticationPrincipal UserPrincipal principal) { return companySettingsService.update(request, principal); }

    @PostMapping(value = "/api/admin/company/assets/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SettingsDtos.UploadResponse uploadCompanyAsset(@PathVariable String type, @RequestPart("file") MultipartFile file, @AuthenticationPrincipal UserPrincipal principal) { return companySettingsService.upload(type, file, principal); }

    @DeleteMapping("/api/admin/company/assets/{type}")
    public SettingsDtos.CompanySettingsResponse deleteCompanyAsset(@PathVariable String type, @AuthenticationPrincipal UserPrincipal principal) { return companySettingsService.deleteAsset(type, principal); }

    @GetMapping("/api/profile")
    public SettingsDtos.ProfileResponse profile(@AuthenticationPrincipal UserPrincipal principal) { return profileService.me(principal); }

    @PutMapping("/api/profile")
    public SettingsDtos.ProfileResponse updateProfile(@RequestBody SettingsDtos.UserProfileRequest request, @AuthenticationPrincipal UserPrincipal principal) { return profileService.update(request, principal); }

    @PutMapping("/api/profile/preferences")
    public SettingsDtos.ProfileResponse preferences(@RequestBody SettingsDtos.UserPreferencesRequest request, @AuthenticationPrincipal UserPrincipal principal) { return profileService.preferences(request, principal); }

    @PutMapping("/api/profile/full")
    public SettingsDtos.ProfileResponse updateFullProfile(@RequestBody SettingsDtos.ProfileUpdateRequest request, @AuthenticationPrincipal UserPrincipal principal) { return profileService.updateFull(request, principal); }

    @PostMapping(value = "/api/profile/assets/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SettingsDtos.UploadResponse uploadProfileAsset(@PathVariable String type, @RequestPart("file") MultipartFile file, @AuthenticationPrincipal UserPrincipal principal) { return profileService.upload(type, file, principal); }

    @DeleteMapping("/api/profile/assets/{type}")
    public SettingsDtos.ProfileResponse deleteProfileAsset(@PathVariable String type, @AuthenticationPrincipal UserPrincipal principal) { return profileService.deleteAsset(type, principal); }

    @GetMapping("/api/assets/{folder}/{name}")
    public ResponseEntity<Resource> asset(@PathVariable String folder, @PathVariable String name) { return ResponseEntity.ok().body(assetStorageService.load(folder, name)); }

    @GetMapping("/api/assets/profiles/{userId}/{name}")
    public ResponseEntity<Resource> profileAsset(@PathVariable String userId, @PathVariable String name) { return ResponseEntity.ok().body(assetStorageService.load("profiles/" + userId, name)); }

    @GetMapping("/api/about")
    public SettingsDtos.AboutResponse about() { return systemInfoService.about(); }

    @GetMapping("/api/admin/backups")
    public SettingsDtos.BackupListResponse backups() { return backupService.list(); }

    @PostMapping("/api/admin/backups")
    public SettingsDtos.BackupItem createBackup(@AuthenticationPrincipal UserPrincipal principal) { return backupService.create(principal); }

    @GetMapping("/api/admin/backups/{name}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String name) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"").contentType(MediaType.TEXT_PLAIN).body(backupService.download(name));
    }

    @DeleteMapping("/api/admin/backups/{name}")
    public void deleteBackup(@PathVariable String name) { backupService.delete(name); }
}
