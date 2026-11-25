package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.DocumentType;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/api/tenders/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TenderDtos.DocumentResponse upload(
            @PathVariable Long id,
            @RequestParam(defaultValue = "RC") DocumentType documentType,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return documentService.upload(id, documentType, file, principal);
    }

    @GetMapping("/api/tenders/{id}/documents")
    public List<TenderDtos.DocumentResponse> list(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.list(id, principal);
    }

    @GetMapping("/api/documents/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        DocumentService.DownloadedDocument downloaded = documentService.download(id, principal);
        String encodedName = java.net.URLEncoder.encode(downloaded.originalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(downloaded.resource());
    }

    @DeleteMapping("/api/documents/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        documentService.delete(id, principal);
    }
}
