package com.aomanagement.service;

import com.aomanagement.dto.PricingDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.InvalidUploadException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderPricingLineRepository;
import com.aomanagement.repository.TenderPricingRepository;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenderPricingService {
    private final TenderRepository tenderRepository;
    private final TenderPricingRepository pricingRepository;
    private final TenderPricingLineRepository lineRepository;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final TenderAuthorizationService authorizationService;
    private final TenderHistoryService historyService;

    @Transactional
    public PricingDtos.PricingResponse get(Long tenderId, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canViewTender(principal, tender)) throw new ForbiddenOperationException("Acces interdit au chiffrage");
        return toResponse(ensurePricing(tender));
    }

    @Transactional
    public PricingDtos.PricingLineResponse addLine(Long tenderId, PricingDtos.PricingLineRequest request, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender)) throw new ForbiddenOperationException("Modification chiffrage interdite");
        TenderPricing pricing = ensurePricing(tender);
        TenderPricingLine line = apply(new TenderPricingLine(), request);
        line.setPricing(pricing);
        if (line.getPosition() == 0) line.setPosition(lineRepository.findByPricingIdOrderByPositionAscIdAsc(pricing.getId()).size() + 1);
        pricing.setStatus(pricing.getStatus() == PricingStatus.NOT_STARTED ? PricingStatus.IN_PROGRESS : pricing.getStatus());
        TenderPricingLine saved = lineRepository.save(line);
        invalidateValidation(pricing);
        recalculate(pricing);
        historyService.record(tender, currentUser(principal), TenderHistoryAction.UPDATED, null, null, null, "Ligne de chiffrage ajoutee.");
        return toLine(saved);
    }

    @Transactional
    public PricingDtos.PricingLineResponse updateLine(Long tenderId, Long lineId, PricingDtos.PricingLineRequest request, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender)) throw new ForbiddenOperationException("Modification chiffrage interdite");
        TenderPricingLine line = lineRepository.findById(lineId).orElseThrow(() -> new NotFoundException("Ligne introuvable"));
        if (!line.getPricing().getTender().getId().equals(tenderId)) throw new NotFoundException("Ligne introuvable");
        apply(line, request);
        invalidateValidation(line.getPricing());
        recalculate(line.getPricing());
        return toLine(line);
    }

    @Transactional
    public void deleteLine(Long tenderId, Long lineId, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender)) throw new ForbiddenOperationException("Modification chiffrage interdite");
        TenderPricingLine line = lineRepository.findById(lineId).orElseThrow(() -> new NotFoundException("Ligne introuvable"));
        if (!line.getPricing().getTender().getId().equals(tenderId)) throw new NotFoundException("Ligne introuvable");
        TenderPricing pricing = line.getPricing();
        lineRepository.delete(line);
        invalidateValidation(pricing);
        recalculate(pricing);
    }

    @Transactional
    public PricingDtos.PricingResponse updateStatus(Long tenderId, PricingDtos.PricingStatusRequest request, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender) && !authorizationService.isAdmin(principal)) throw new ForbiddenOperationException("Modification chiffrage interdite");
        if (request.status() == PricingStatus.VALIDATED && !authorizationService.isAdmin(principal)) {
            throw new ForbiddenOperationException("Seul un administrateur peut valider le chiffrage. Passez-le en \"A valider\".");
        }
        TenderPricing pricing = ensurePricing(tender);
        pricing.setStatus(request.status());
        if (request.notes() != null) pricing.setNotes(request.notes());
        if (request.status() != PricingStatus.VALIDATED) {
            pricing.setValidatedAt(null);
            pricing.setValidatedBy(null);
        }
        if (request.status() == PricingStatus.VALIDATED) {
            pricing.setValidatedAt(LocalDateTime.now());
            pricing.setValidatedBy(currentUser(principal));
            historyService.record(tender, currentUser(principal), TenderHistoryAction.UPDATED, "pricing", null, "VALIDATED", "Chiffrage valide.");
        }
        recalculate(pricing);
        return toResponse(pricing);
    }

    @Transactional(readOnly = true)
    public List<PricingDtos.PricingResponse> myPricing(Long employeeId) {
        return pricingRepository.findByAssignedPricingManagerIdOrderByTenderDeadlineAsc(employeeId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PricingDtos.PricingImportPreview previewImport(Long tenderId, MultipartFile file, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender)) throw new ForbiddenOperationException("Import chiffrage interdit");
        if (file == null || file.isEmpty()) throw new InvalidUploadException("Fichier Excel obligatoire");
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // Colonnes par defaut (ancien format) : N | Designation | Unite | Quantite | PU
            int headerRow = 0, colNumber = 0, colReference = -1, colDesignation = 1, colUnit = 2, colQuantity = 3, colPrice = 4;
            // Detection automatique de la ligne d'en-tete (permet de reimporter le fichier exporte)
            for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 20); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                int designation = -1, number = -1, reference = -1, unit = -1, quantity = -1, price = -1;
                for (int c = 0; c < Math.max(0, row.getLastCellNum()); c++) {
                    String h = normalize(cell(row, c));
                    if (h.isEmpty()) continue;
                    if (h.startsWith("designation") || h.startsWith("libelle") || h.equals("description")) designation = c;
                    else if (h.equals("n") || h.startsWith("n°") || h.startsWith("no") || h.startsWith("num") || h.equals("#") || h.startsWith("art")) number = c;
                    else if (h.startsWith("ref")) reference = c;
                    else if (h.startsWith("unite") || h.equals("u")) unit = c;
                    else if (h.startsWith("quantite") || h.startsWith("qte") || h.startsWith("qty")) quantity = c;
                    else if ((h.startsWith("prix") || h.startsWith("pu") || h.contains("unitaire")) && !h.contains("total")) price = c;
                }
                if (designation >= 0) {
                    headerRow = i; colDesignation = designation; colNumber = number; colReference = reference;
                    colUnit = unit; colQuantity = quantity; colPrice = price;
                    break;
                }
            }
            List<PricingDtos.PricingLineRequest> lines = new java.util.ArrayList<>();
            boolean containsPrices = false;
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String designation = cell(row, colDesignation);
                if (designation == null || designation.isBlank()) continue;
                BigDecimal quantity = colQuantity < 0 ? BigDecimal.ONE : decimal(row.getCell(colQuantity));
                BigDecimal price = colPrice < 0 ? BigDecimal.ZERO : decimal(row.getCell(colPrice));
                containsPrices = containsPrices || price.compareTo(BigDecimal.ZERO) > 0;
                String number = colNumber < 0 ? String.valueOf(lines.size() + 1) : text(row, colNumber);
                String reference = colReference < 0 ? number : text(row, colReference);
                lines.add(new PricingDtos.PricingLineRequest(number, reference, designation.trim(), colUnit < 0 ? null : cell(row, colUnit), quantity, price, null, lines.size() + 1));
            }
            if (lines.isEmpty()) throw new InvalidUploadException("Aucune ligne trouvee : le fichier doit contenir une colonne \"Designation\"");
            return new PricingDtos.PricingImportPreview(lines, containsPrices);
        } catch (InvalidUploadException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidUploadException("Import Excel impossible : fichier .xlsx/.xls illisible");
        }
    }

    @Transactional
    public PricingDtos.PricingResponse applyImport(Long tenderId, PricingDtos.PricingImportPreview preview, boolean replace, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canEditPricing(principal, tender)) throw new ForbiddenOperationException("Import chiffrage interdit");
        TenderPricing pricing = ensurePricing(tender);
        if (!replace && lineRepository.findByPricingIdOrderByPositionAscIdAsc(pricing.getId()).stream().anyMatch(l -> l.getUnitPrice().compareTo(BigDecimal.ZERO) > 0)) {
            throw new ConflictException("Le bordereau contient deja des prix. Confirmez le remplacement pour importer.");
        }
        if (preview == null || preview.lines() == null || preview.lines().isEmpty()) throw new InvalidUploadException("Aucune ligne a importer");
        lineRepository.deleteByPricingId(pricing.getId());
        preview.lines().forEach(line -> {
            TenderPricingLine entity = apply(new TenderPricingLine(), line);
            entity.setPricing(pricing);
            lineRepository.save(entity);
        });
        if (pricing.getStatus() == PricingStatus.NOT_STARTED) pricing.setStatus(PricingStatus.IN_PROGRESS);
        invalidateValidation(pricing);
        recalculate(pricing);
        historyService.record(tender, currentUser(principal), TenderHistoryAction.UPDATED, "pricing", null, null, preview.lines().size() + " lignes de chiffrage importees depuis Excel.");
        return toResponse(pricing);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> export(Long tenderId, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canViewTender(principal, tender)) throw new ForbiddenOperationException("Export chiffrage interdit");
        TenderPricing pricing = ensurePricing(tender);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bordereau");
            sheet.createRow(0).createCell(0).setCellValue("Reference AO");
            sheet.getRow(0).createCell(1).setCellValue(tender.getReference());
            sheet.createRow(1).createCell(0).setCellValue("Organisme");
            sheet.getRow(1).createCell(1).setCellValue(tender.getOrganization());
            sheet.createRow(2).createCell(0).setCellValue("Responsable chiffrage");
            sheet.getRow(2).createCell(1).setCellValue(pricing.getAssignedPricingManager() == null ? "" : pricing.getAssignedPricingManager().getFirstName() + " " + pricing.getAssignedPricingManager().getLastName());
            Row header = sheet.createRow(4);
            String[] titles = {"N", "Reference", "Designation", "Unite", "Quantite", "Prix unitaire", "Total"};
            for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
            int rowIndex = 5;
            for (TenderPricingLine line : lineRepository.findByPricingIdOrderByPositionAscIdAsc(pricing.getId())) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(line.getLineNumber());
                row.createCell(1).setCellValue(line.getReference());
                row.createCell(2).setCellValue(line.getDesignation());
                row.createCell(3).setCellValue(line.getUnit());
                row.createCell(4).setCellValue(line.getQuantity().doubleValue());
                row.createCell(5).setCellValue(line.getUnitPrice().doubleValue());
                row.createCell(6).setCellValue(line.getTotalPrice().doubleValue());
            }
            workbook.write(out);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=pricing-" + tender.getReference() + ".xlsx")
                    .body(new ByteArrayResource(out.toByteArray()));
        } catch (Exception ex) {
            throw new IllegalStateException("Export chiffrage impossible");
        }
    }

    public TenderPricing ensurePricing(Tender tender) {
        return pricingRepository.findByTenderId(tender.getId()).orElseGet(() -> {
            TenderPricing pricing = new TenderPricing();
            pricing.setTender(tender);
            pricing.setAssignedPricingManager(null);
            return pricingRepository.save(pricing);
        });
    }

    private TenderPricingLine apply(TenderPricingLine line, PricingDtos.PricingLineRequest request) {
        line.setLineNumber(request.lineNumber());
        line.setReference(request.reference());
        line.setDesignation(request.designation());
        line.setUnit(request.unit());
        line.setQuantity(request.quantity() == null ? BigDecimal.ZERO : request.quantity());
        line.setUnitPrice(request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice());
        line.setNotes(request.notes());
        line.setPosition(request.position() == null ? 0 : request.position());
        line.calculateTotal();
        return line;
    }

    /** Toute modification d'un chiffrage valide impose une nouvelle validation. */
    private void invalidateValidation(TenderPricing pricing) {
        if (pricing.getStatus() == PricingStatus.VALIDATED) {
            pricing.setStatus(PricingStatus.REVISION_REQUIRED);
            pricing.setValidatedAt(null);
            pricing.setValidatedBy(null);
        }
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private String text(Row row, int index) {
        Cell c = row.getCell(index);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
        }
        String v = c.toString().trim();
        return v.isEmpty() ? null : v;
    }

    private void recalculate(TenderPricing pricing) {
        BigDecimal total = lineRepository.findByPricingIdOrderByPositionAscIdAsc(pricing.getId()).stream().map(TenderPricingLine::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        pricing.setFinalTotal(total);
    }

    private PricingDtos.PricingResponse toResponse(TenderPricing pricing) {
        List<PricingDtos.PricingLineResponse> lines = lineRepository.findByPricingIdOrderByPositionAscIdAsc(pricing.getId()).stream().map(this::toLine).toList();
        return new PricingDtos.PricingResponse(pricing.getId(), pricing.getTender().getId(), pricing.getStatus(), mapperService.toSimpleUser(pricing.getAssignedPricingManager()), pricing.getEstimatedTotal() != null ? pricing.getEstimatedTotal() : pricing.getTender().getEstimatedAmount(), pricing.getFinalTotal(), pricing.getCurrency(), pricing.getNotes(), pricing.getValidatedAt(), mapperService.toSimpleUser(pricing.getValidatedBy()), lines);
    }

    private PricingDtos.PricingLineResponse toLine(TenderPricingLine line) {
        return new PricingDtos.PricingLineResponse(line.getId(), line.getLineNumber(), line.getReference(), line.getDesignation(), line.getUnit(), line.getQuantity(), line.getUnitPrice(), line.getTotalPrice(), line.getNotes(), line.getPosition());
    }

    private Tender tender(Long id) { return tenderRepository.findById(id).orElseThrow(() -> new NotFoundException("Appel d'offres introuvable")); }
    private User currentUser(UserPrincipal principal) { return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable")); }
    private String cell(Row row, int index) { Cell c = row.getCell(index); return c == null ? null : c.toString(); }
    private BigDecimal decimal(Cell cell) { try { return cell == null ? BigDecimal.ZERO : BigDecimal.valueOf(cell.getNumericCellValue()); } catch (Exception ex) { return BigDecimal.ZERO; } }
}
