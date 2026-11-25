package com.aomanagement.service;

import com.aomanagement.dto.StatisticsDtos;
import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Tender;
import com.aomanagement.entity.User;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {
    private final TenderService tenderService;
    private final TenderRepository tenderRepository;
    private final StatisticsService statisticsService;
    private final UserRepository userRepository;
    private final TenderHistoryService historyService;
    private final CompanySettingsService companySettingsService;

    @Transactional
    public byte[] tendersExcel(TenderDtos.TenderSearchCriteria criteria, UserPrincipal principal) {
        List<Tender> tenders = tenderRepository.findAll(tenderService.buildSpecification(criteria));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Appels d'offres");
            com.aomanagement.entity.CompanySettings settings = companySettingsService.entity();
            Row meta1 = sheet.createRow(0);
            meta1.createCell(0).setCellValue(settings.getCompanyName() == null ? "" : settings.getCompanyName());
            meta1.createCell(1).setCellValue(settings.getApplicationName());
            Row meta2 = sheet.createRow(1);
            meta2.createCell(0).setCellValue("Date generation");
            meta2.createCell(1).setCellValue(java.time.LocalDateTime.now().toString());
            Row meta3 = sheet.createRow(2);
            meta3.createCell(0).setCellValue("Utilisateur");
            meta3.createCell(1).setCellValue(principal.getUsername());
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            String[] headers = {"Reference", "Numero marche", "Objet", "Organisme", "Date publication", "Date limite", "Responsable", "Statut", "Resultat", "Priorite", "Montant estime", "Montant gagne", "Date resultat"};
            Row header = sheet.createRow(4);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 5;
            for (Tender tender : tenders) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(tender.getReference());
                row.createCell(1).setCellValue(value(tender.getMarketNumber()));
                row.createCell(2).setCellValue(tender.getTitle());
                row.createCell(3).setCellValue(tender.getOrganization());
                row.createCell(4).setCellValue(date(tender.getPublicationDate()));
                row.createCell(5).setCellValue(date(tender.getDeadline()));
                row.createCell(6).setCellValue(tender.getAssignedEmployee() == null ? "" : tender.getAssignedEmployee().getFirstName() + " " + tender.getAssignedEmployee().getLastName());
                row.createCell(7).setCellValue(tender.getStatus().name());
                row.createCell(8).setCellValue(tender.getResult().name());
                row.createCell(9).setCellValue(tender.getPriority().name());
                row.createCell(10).setCellValue(number(tender.getEstimatedAmount()));
                row.createCell(11).setCellValue(number(tender.getFinalAmount()));
                row.createCell(12).setCellValue(date(tender.getResultDate()));
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(4, Math.max(rowIndex - 1, 4), 0, headers.length - 1));
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            audit(principal, "EXPORT_EXCEL", "Export Excel des appels d'offres.");
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Export Excel impossible", ex);
        }
    }

    @Transactional
    public byte[] reportPdf(StatisticsDtos.StatisticsFilter filter, UserPrincipal principal) {
        StatisticsDtos.OverviewResponse overview = statisticsService.overview(filter);
        List<Tender> tenders = tenderRepository.findAll().stream().limit(30).toList();
        com.aomanagement.entity.CompanySettings settings = companySettingsService.entity();
        User generator = userRepository.findById(principal.getId()).orElse(null);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float y = 780;
                y = line(content, bold, 12, 50, y, settings.getCompanyName() == null ? value(settings.getApplicationName()) : settings.getCompanyName());
                y = line(content, regular, 9, 50, y, (settings.getAddress() == null ? "" : settings.getAddress() + " | ") + (settings.getPhone() == null ? "" : "Tel: " + settings.getPhone() + " | ") + (settings.getEmail() == null ? "" : settings.getEmail()));
                y = line(content, regular, 9, 50, y, "ICE: " + value(settings.getIce()) + " | RC: " + value(settings.getRc()) + " | Genere le: " + java.time.LocalDateTime.now() + " | Par: " + (generator == null ? principal.getUsername() : generator.getFirstName() + " " + generator.getLastName()));
                y -= 15;
                content.beginText();
                content.setFont(bold, 18);
                content.newLineAtOffset(50, y);
                content.showText("Rapport des appels d'offres");
                content.endText();
                y -= 35;
                y = line(content, regular, 11, 50, y, "Periode analysee: " + (filter.dateFrom() == null ? "debut" : filter.dateFrom()) + " -> " + (filter.dateTo() == null ? "aujourd'hui" : filter.dateTo()));
                y = line(content, regular, 11, 50, y, "Total: " + overview.totalTenders() + " | Gagnes: " + overview.won() + " | Perdus: " + overview.lost() + " | Taux: " + overview.winRate() + "% | Montant remporte: " + overview.totalWonAmount() + " DH");
                y -= 15;
                y = line(content, bold, 11, 50, y, "Reference | Objet | Organisme | Responsable | Resultat | Montant");
                for (Tender tender : tenders) {
                    String responsible = tender.getAssignedEmployee() == null ? "-" : tender.getAssignedEmployee().getFirstName() + " " + tender.getAssignedEmployee().getLastName();
                    String text = truncate(tender.getReference() + " | " + tender.getTitle() + " | " + tender.getOrganization() + " | " + responsible + " | " + tender.getResult() + " | " + number(tender.getFinalAmount()), 115);
                    y = line(content, regular, 9, 50, y, text);
                    if (y < 60) break;
                }
            }
            document.save(out);
            audit(principal, "EXPORT_PDF", "Export PDF du rapport des appels d'offres.");
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Export PDF impossible", ex);
        }
    }

    private void audit(UserPrincipal principal, String action, String description) {
        User user = userRepository.findById(principal.getId()).orElse(null);
        historyService.record(null, user, com.aomanagement.entity.TenderHistoryAction.UPDATED, action, null, null, description);
    }

    private float line(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText((text == null ? "" : text).replaceAll("[\\r\\n]", " "));
        content.endText();
        return y - 17;
    }

    private String value(String value) { return value == null ? "" : value; }
    private String date(LocalDate date) { return date == null ? "" : date.toString(); }
    private double number(BigDecimal value) { return value == null ? 0D : value.doubleValue(); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 3) + "..."; }
}
