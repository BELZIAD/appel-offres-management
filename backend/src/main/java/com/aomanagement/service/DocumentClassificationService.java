package com.aomanagement.service;

import org.springframework.stereotype.Service;

@Service
public class DocumentClassificationService {
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "UNKNOWN";
        boolean arabic = text.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        boolean french = text.toLowerCase().matches("(?s).*(appel d.offres|caution|march[eé]|offre|d[eé]lai|r[eé]glement).*");
        if (arabic && french) return "FR_AR";
        if (arabic) return "AR";
        if (french) return "FR";
        return "UNKNOWN";
    }
}
