package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.DocumentVersionService;
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
public class DocumentVersionController {
    private final DocumentVersionService documentVersionService;

    @PostMapping(value = "/api/tenders/{tenderId}/documents/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SprintFourDtos.DocumentVersionResponse addVersion(@PathVariable Long tenderId, @PathVariable Long documentId, @RequestPart("file") MultipartFile file, @RequestParam(required = false) String comment, @AuthenticationPrincipal UserPrincipal principal) {
        return documentVersionService.addVersion(tenderId, documentId, file, comment, principal);
    }

    @GetMapping("/api/tenders/{tenderId}/documents/{documentId}/versions")
    public List<SprintFourDtos.DocumentVersionResponse> versions(@PathVariable Long tenderId, @PathVariable Long documentId, @AuthenticationPrincipal UserPrincipal principal) {
        return documentVersionService.list(tenderId, documentId, principal);
    }

    @GetMapping("/api/document-versions/{versionId}/download")
    public ResponseEntity<?> download(@PathVariable Long versionId, @AuthenticationPrincipal UserPrincipal principal) {
        DocumentVersionService.DownloadedVersion downloaded = documentVersionService.download(versionId, principal);
        String encodedName = java.net.URLEncoder.encode(downloaded.originalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(downloaded.resource());
    }

    @DeleteMapping("/api/document-versions/{versionId}")
    public void delete(@PathVariable Long versionId, @AuthenticationPrincipal UserPrincipal principal) {
        documentVersionService.delete(versionId, principal);
    }
}
