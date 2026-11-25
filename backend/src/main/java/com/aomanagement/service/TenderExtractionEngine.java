package com.aomanagement.service;

import com.aomanagement.entity.DocumentAnalysis;

public interface TenderExtractionEngine {
    String version();
    void extract(DocumentAnalysis analysis, String normalizedText);
}
