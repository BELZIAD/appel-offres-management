package com.aomanagement.service;

import com.aomanagement.entity.DocumentAnalysis;
import com.aomanagement.entity.ExtractedTenderInformation;
import com.aomanagement.repository.ExtractedTenderInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TenderInformationExtractionService {
    private final ExtractedTenderInformationRepository informationRepository;

    public void extract(DocumentAnalysis analysis, String text) {
        findDate(text, "(date limite|remise des offres|depot des offres|dépôt des offres|submission deadline)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", "submissionDeadline", analysis);
        findDate(text, "(ouverture des plis|opening)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", "openingDate", analysis);
        findDate(text, "(publication|publie|publié)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", "publicationDate", analysis);
        findMoney(text, "(estimation|montant estime|montant estimé|budget)[^\\n]{0,120}?([0-9][0-9\\s.,]*)\\s*(dh|mad|dhs|dirhams)?", "estimatedAmount", analysis);
        findMoney(text, "(caution provisoire|garantie provisoire|provisional guarantee)[^\\n]{0,120}?([0-9][0-9\\s.,]*)\\s*(dh|mad|dhs|dirhams)?", "provisionalGuarantee", analysis);
        findText(text, "(objet|title)\\s*[:\\-]\\s*([^\\n]{8,180})", "title", analysis);
        findText(text, "(maitre d ouvrage|maître d ouvrage|organisme|acheteur)\\s*[:\\-]\\s*([^\\n]{3,160})", "organization", analysis);
        findText(text, "(appel d.offres n|ao n|reference|référence)\\s*[:\\-]?\\s*([A-Za-z0-9/\\-_.]{3,80})", "reference", analysis);
        findText(text, "(delai d.execution|délai d.exécution|execution period)\\s*[:\\-]?\\s*([^\\n]{3,80})", "executionPeriod", analysis);
    }

    public ExtractedTenderInformation save(DocumentAnalysis analysis, String field, String value, double confidence, Integer page, String excerpt) {
        ExtractedTenderInformation info = new ExtractedTenderInformation();
        info.setAnalysis(analysis);
        info.setFieldName(field);
        info.setFieldValue(value);
        info.setConfidence(BigDecimal.valueOf(confidence));
        info.setSourcePage(page);
        info.setSourceExcerpt(shortExcerpt(excerpt));
        return informationRepository.save(info);
    }

    private void findDate(String text, String regex, String field, DocumentAnalysis analysis) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (matcher.find()) {
            LocalDate date = parseDate(matcher.group(2));
            if (date != null) save(analysis, field, date.toString(), 0.86, pageOf(text, matcher.start()), matcher.group());
        }
    }

    private void findMoney(String text, String regex, String field, DocumentAnalysis analysis) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (matcher.find()) {
            String number = matcher.group(2).replace(" ", "").replace(",", ".");
            save(analysis, field, number, 0.82, pageOf(text, matcher.start()), matcher.group());
        }
    }

    private void findText(String text, String regex, String field, DocumentAnalysis analysis) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (matcher.find()) {
            save(analysis, field, matcher.group(2).trim(), 0.72, pageOf(text, matcher.start()), matcher.group());
        }
    }

    LocalDate parseDate(String value) {
        for (String pattern : new String[]{"d/M/yyyy", "dd/MM/yyyy", "d-M-yyyy", "dd-MM-yyyy"}) {
            try { return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern(pattern, Locale.FRANCE)); } catch (Exception ignored) {}
        }
        return null;
    }

    int pageOf(String text, int offset) {
        int page = 1;
        Matcher matcher = Pattern.compile("--- PAGE (\\d+) ---").matcher(text.substring(0, Math.max(0, offset)));
        while (matcher.find()) page = Integer.parseInt(matcher.group(1));
        return page;
    }

    private String shortExcerpt(String value) {
        String clean = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return clean.length() <= 450 ? clean : clean.substring(0, 447) + "...";
    }
}
