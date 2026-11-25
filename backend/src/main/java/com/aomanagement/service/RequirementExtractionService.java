package com.aomanagement.service;

import com.aomanagement.entity.*;
import com.aomanagement.repository.ExtractedRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequirementExtractionService {
    private final ExtractedRequirementRepository requirementRepository;
    private final TenderInformationExtractionService informationExtractionService;

    public void extract(DocumentAnalysis analysis, String text) {
        Map<String, RequirementCategory> patterns = new LinkedHashMap<>();
        patterns.put("Attestation fiscale", RequirementCategory.CERTIFICATE);
        patterns.put("Attestation CNSS", RequirementCategory.CERTIFICATE);
        patterns.put("Registre de commerce", RequirementCategory.ADMINISTRATIVE);
        patterns.put("Caution provisoire", RequirementCategory.GUARANTEE);
        patterns.put("Memoire technique", RequirementCategory.TECHNICAL);
        patterns.put("Mémoire technique", RequirementCategory.TECHNICAL);
        patterns.put("Offre financiere", RequirementCategory.FINANCIAL);
        patterns.put("Offre financière", RequirementCategory.FINANCIAL);
        patterns.put("Bordereau des prix", RequirementCategory.FINANCIAL);
        for (Map.Entry<String, RequirementCategory> entry : patterns.entrySet()) {
            int index = text.toLowerCase().indexOf(entry.getKey().toLowerCase());
            if (index >= 0) {
                ExtractedRequirement requirement = new ExtractedRequirement();
                requirement.setAnalysis(analysis);
                requirement.setLabel(entry.getKey().replace("Memoire", "Mémoire").replace("financiere", "financière"));
                requirement.setDescription("Piece demandee detectee dans le document.");
                requirement.setCategory(entry.getValue());
                requirement.setRequired(true);
                requirement.setSourcePage(informationExtractionService.pageOf(text, index));
                requirement.setConfidence(BigDecimal.valueOf(0.78));
                requirement.setStatus(RequirementStatus.DETECTED);
                requirementRepository.save(requirement);
            }
        }
    }
}
