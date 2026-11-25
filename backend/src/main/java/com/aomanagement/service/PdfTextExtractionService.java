package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.entity.ExtractionMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
public class PdfTextExtractionService {
    public AnalysisDtos.PdfExtractionResult extractText(Path file) {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            int pages = document.getNumberOfPages();
            StringBuilder text = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append("\n--- PAGE ").append(page).append(" ---\n");
                text.append(stripper.getText(document));
            }
            String extracted = text.toString();
            int usefulLength = extracted.replaceAll("\\s+", "").length();
            boolean requiresOcr = pages > 0 && usefulLength < Math.max(40, pages * 20);
            return new AnalysisDtos.PdfExtractionResult(extracted, pages, usefulLength, requiresOcr, ExtractionMethod.NATIVE_TEXT);
        } catch (Exception ex) {
            log.warn("PDF native extraction failed for {}: {}", file, ex.getMessage());
            return new AnalysisDtos.PdfExtractionResult("", 0, 0, false, ExtractionMethod.FAILED);
        }
    }
}
