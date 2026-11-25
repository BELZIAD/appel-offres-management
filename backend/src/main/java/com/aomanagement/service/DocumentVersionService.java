package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.InvalidUploadException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.DocumentVersionRepository;
import com.aomanagement.repository.TenderDocumentRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentVersionService {
    private final DocumentVersionRepository versionRepository;
    private final TenderDocumentRepository documentRepository;
    private final TenderService tenderService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final ActivityLogService activityLogService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<SprintFourDtos.DocumentVersionResponse> list(Long tenderId, Long documentId, UserPrincipal principal) {
        TenderDocument document = securedDocument(tenderId, documentId, principal);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream().map(mapperService::toDocumentVersionResponse).toList();
    }

    @Transactional
    public SprintFourDtos.DocumentVersionResponse addVersion(Long tenderId, Long documentId, MultipartFile file, String comment, UserPrincipal principal) {
        TenderDocument document = securedDocument(tenderId, documentId, principal);
        validatePdf(file);
        User actor = userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        versionRepository.findWithLockByDocumentIdOrderByVersionNumberDesc(document.getId());
        int next = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId()).map(v -> v.getVersionNumber() + 1).orElse(1);
        DocumentVersion version = storeVersion(document, next, file, comment, actor);
        document.setFileName(version.getStoredFileName());
        document.setOriginalFileName(version.getOriginalFileName());
        document.setFilePath(version.getFilePath());
        document.setFileSize(version.getFileSize());
        activityLogService.record(actor, document.getTender(), "DOCUMENT_VERSION", version.getId(), "DOCUMENT_VERSION_ADDED", actor.getFirstName() + " a ajoute la version " + version.getVersionNumber() + " de " + document.getOriginalFileName() + ".");
        return mapperService.toDocumentVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public DownloadedVersion download(Long versionId, UserPrincipal principal) {
        DocumentVersion version = versionRepository.findById(versionId).orElseThrow(() -> new NotFoundException("Version introuvable"));
        tenderService.findSecuredEntity(version.getDocument().getTender().getId(), principal);
        try {
            Resource resource = new UrlResource(Path.of(version.getFilePath()).normalize().toUri());
            if (!resource.exists()) {
                throw new NotFoundException("Fichier introuvable");
            }
            return new DownloadedVersion(resource, version.getOriginalFileName());
        } catch (MalformedURLException ex) {
            throw new NotFoundException("Fichier introuvable");
        }
    }

    @Transactional
    public void delete(Long versionId, UserPrincipal principal) {
        if (isEmployee(principal)) {
            throw new ForbiddenOperationException("Suppression reservee a l'administrateur");
        }
        DocumentVersion version = versionRepository.findById(versionId).orElseThrow(() -> new NotFoundException("Version introuvable"));
        tenderService.findSecuredEntity(version.getDocument().getTender().getId(), principal);
        versionRepository.delete(version);
        try {
            Files.deleteIfExists(Path.of(version.getFilePath()).normalize());
        } catch (IOException ignored) {
        }
    }

    public DocumentVersion storeVersion(TenderDocument document, int versionNumber, MultipartFile file, String comment, User actor) {
        validatePdf(file);
        try {
            String original = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path directory = root.resolve(Path.of("tenders", document.getTender().getId().toString(), "documents", document.getId().toString())).normalize();
            if (!directory.startsWith(root)) {
                throw new InvalidUploadException("Chemin de stockage invalide");
            }
            Files.createDirectories(directory);
            String storedName = "v" + versionNumber + "_" + UUID.randomUUID() + ".pdf";
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) {
                throw new InvalidUploadException("Chemin de stockage invalide");
            }
            file.transferTo(target);
            DocumentVersion version = new DocumentVersion();
            version.setDocument(document);
            version.setVersionNumber(versionNumber);
            version.setStoredFileName(storedName);
            version.setOriginalFileName(original);
            version.setFilePath(target.toString());
            version.setFileSize(file.getSize());
            version.setUploadedBy(actor);
            version.setComment(comment);
            return versionRepository.save(version);
        } catch (IOException ex) {
            throw new InvalidUploadException("Upload impossible");
        }
    }

    private TenderDocument securedDocument(Long tenderId, Long documentId, UserPrincipal principal) {
        TenderDocument document = documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document introuvable"));
        if (!document.getTender().getId().equals(tenderId)) {
            throw new NotFoundException("Document introuvable");
        }
        tenderService.findSecuredEntity(tenderId, principal);
        return document;
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Fichier PDF obligatoire");
        }
        String original = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new InvalidUploadException("Seuls les fichiers PDF sont acceptes");
        }
        try {
            String header = new String(file.getInputStream().readNBytes(5));
            if (!header.startsWith("%PDF-")) {
                throw new InvalidUploadException("Le fichier PDF est invalide");
            }
        } catch (IOException ex) {
            throw new InvalidUploadException("Le fichier PDF est invalide");
        }
    }

    private boolean isEmployee(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    public record DownloadedVersion(Resource resource, String originalFileName) {}
}
