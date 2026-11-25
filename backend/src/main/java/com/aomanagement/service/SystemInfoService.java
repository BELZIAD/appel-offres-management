package com.aomanagement.service;

import com.aomanagement.dto.SettingsDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Page "A propos" : uniquement les informations publiques de l'application.
 * Les informations techniques (version Java, OS, memoire, SMTP, pile logicielle...) ne sont plus exposees.
 */
@Service
@RequiredArgsConstructor
public class SystemInfoService {
    private final CompanySettingsService companySettingsService;

    public SettingsDtos.AboutResponse about() {
        return new SettingsDtos.AboutResponse(companySettingsService.current());
    }
}
