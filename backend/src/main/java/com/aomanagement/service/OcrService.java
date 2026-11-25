package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;

import java.nio.file.Path;

public interface OcrService {
    boolean isAvailable();
    AnalysisDtos.PdfExtractionResult extractText(Path file);
}
