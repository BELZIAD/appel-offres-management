package com.aomanagement.controller;

import com.aomanagement.dto.StatisticsDtos;
import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Priority;
import com.aomanagement.entity.TenderResult;
import com.aomanagement.entity.TenderStatus;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/exports")
@RequiredArgsConstructor
public class ExportController {
    private final ExportService exportService;

    @GetMapping("/tenders.xlsx")
    public ResponseEntity<byte[]> excel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenderStatus status,
            @RequestParam(required = false) TenderResult result,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) LocalDate deadlineFrom,
            @RequestParam(required = false) LocalDate deadlineTo,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        byte[] bytes = exportService.tendersExcel(new TenderDtos.TenderSearchCriteria(search, status, result, priority, employeeId, organization, deadlineFrom, deadlineTo), principal);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=appels-offres-" + LocalDate.now() + ".xlsx")
                .body(bytes);
    }

    @GetMapping("/report.pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo, @AuthenticationPrincipal UserPrincipal principal) {
        byte[] bytes = exportService.reportPdf(new StatisticsDtos.StatisticsFilter(dateFrom, dateTo), principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-appels-offres-" + LocalDate.now() + ".pdf")
                .body(bytes);
    }
}
