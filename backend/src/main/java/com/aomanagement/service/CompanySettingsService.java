package com.aomanagement.service;

import com.aomanagement.dto.SettingsDtos;
import com.aomanagement.entity.CompanySettings;
import com.aomanagement.entity.User;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.CompanySettingsRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {
    private final CompanySettingsRepository repository;
    private final UserRepository userRepository;
    private final AssetStorageService assetStorageService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public SettingsDtos.CompanySettingsResponse current() {
        return toResponse(entity());
    }

    @Transactional
    public SettingsDtos.CompanySettingsResponse update(SettingsDtos.CompanySettingsRequest request, UserPrincipal principal) {
        CompanySettings s = entity();
        if (request.companyName() != null) s.setCompanyName(request.companyName().isBlank() ? null : request.companyName().trim());
        if (request.applicationName() != null) s.setApplicationName(request.applicationName());
        if (request.primaryColor() != null) s.setPrimaryColor(request.primaryColor());
        if (request.secondaryColor() != null) s.setSecondaryColor(request.secondaryColor());
        if (request.accentColor() != null) s.setAccentColor(request.accentColor());
        if (request.dangerColor() != null) s.setDangerColor(request.dangerColor());
        if (request.warningColor() != null) s.setWarningColor(request.warningColor());
        if (request.successColor() != null) s.setSuccessColor(request.successColor());
        if (request.sidebarColor() != null) s.setSidebarColor(request.sidebarColor());
        if (request.headerColor() != null) s.setHeaderColor(request.headerColor());
        s.setAddress(request.address()); s.setCity(request.city()); s.setPostalCode(request.postalCode()); s.setCountry(request.country());
        s.setPhone(request.phone()); s.setMobile(request.mobile()); s.setEmail(request.email()); s.setWebsite(request.website());
        s.setIce(request.ice()); s.setIfNumber(request.ifNumber()); s.setRc(request.rc()); s.setPatente(request.patente()); s.setTaxIdentifier(request.taxIdentifier());
        if (request.currency() != null) s.setCurrency(request.currency());
        if (request.timezone() != null) s.setTimezone(request.timezone());
        if (request.language() != null) s.setLanguage(request.language());
        s.setLinkedin(request.linkedin()); s.setFacebook(request.facebook()); s.setInstagram(request.instagram()); s.setYoutube(request.youtube());
        audit(principal, "COMPANY_SETTINGS_UPDATED", "Parametres entreprise modifies.");
        return toResponse(s);
    }

    @Transactional
    public SettingsDtos.UploadResponse upload(String type, MultipartFile file, UserPrincipal principal) {
        CompanySettings s = entity();
        String previous = switch (type) {
            case "logo" -> s.getCompanyLogo();
            case "logo-dark" -> s.getCompanyLogoDark();
            case "favicon" -> s.getFavicon();
            default -> throw new NotFoundException("Type asset introuvable");
        };
        String url = assetStorageService.store("branding", file);
        switch (type) {
            case "logo" -> s.setCompanyLogo(url);
            case "logo-dark" -> s.setCompanyLogoDark(url);
            case "favicon" -> s.setFavicon(url);
            default -> throw new NotFoundException("Type asset introuvable");
        }
        assetStorageService.delete(previous);
        audit(principal, "COMPANY_ASSET_UPDATED", "Asset entreprise modifie: " + type + ".");
        return new SettingsDtos.UploadResponse(url);
    }

    @Transactional
    public SettingsDtos.CompanySettingsResponse deleteAsset(String type, UserPrincipal principal) {
        CompanySettings s = entity();
        String previous = switch (type) {
            case "logo" -> {
                String value = s.getCompanyLogo();
                s.setCompanyLogo(null);
                yield value;
            }
            case "logo-dark" -> {
                String value = s.getCompanyLogoDark();
                s.setCompanyLogoDark(null);
                yield value;
            }
            case "favicon" -> {
                String value = s.getFavicon();
                s.setFavicon(null);
                yield value;
            }
            default -> throw new NotFoundException("Type asset introuvable");
        };
        assetStorageService.delete(previous);
        audit(principal, "COMPANY_ASSET_DELETED", "Asset entreprise supprime: " + type + ".");
        return toResponse(s);
    }

    public CompanySettings entity() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            CompanySettings s = new CompanySettings();
            s.setApplicationName("Gestion des Appels d'Offres");
            return repository.save(s);
        });
    }

    public SettingsDtos.CompanySettingsResponse toResponse(CompanySettings s) {
        return new SettingsDtos.CompanySettingsResponse(s.getId(), s.getCompanyName(), s.getApplicationName(), s.getCompanyLogo(), s.getCompanyLogoDark(), s.getFavicon(), s.getPrimaryColor(), s.getSecondaryColor(), s.getAccentColor(), s.getDangerColor(), s.getWarningColor(), s.getSuccessColor(), s.getSidebarColor(), s.getHeaderColor(), s.getAddress(), s.getCity(), s.getPostalCode(), s.getCountry(), s.getPhone(), s.getMobile(), s.getEmail(), s.getWebsite(), s.getIce(), s.getIfNumber(), s.getRc(), s.getPatente(), s.getTaxIdentifier(), s.getCurrency(), s.getTimezone(), s.getLanguage(), s.getLinkedin(), s.getFacebook(), s.getInstagram(), s.getYoutube(), s.getUpdatedAt());
    }

    private void audit(UserPrincipal principal, String action, String description) {
        User user = principal == null ? null : userRepository.findById(principal.getId()).orElse(null);
        activityLogService.record(user, null, "COMPANY_SETTINGS", 1L, action, description);
    }
}
