package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.entity.ExtractionMethod;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class TesseractOcrService implements OcrService {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public AnalysisDtos.PdfExtractionResult extractText(Path file) {
        return new AnalysisDtos.PdfExtractionResult("", 0, 0, true, ExtractionMethod.OCR);
    }
}
