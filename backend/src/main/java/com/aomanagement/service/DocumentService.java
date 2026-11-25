package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.InvalidUploadException;
import com.aomanagement.exception.NotFoundException;
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
public class DocumentService {
    private final TenderDocumentRepository documentRepository;
    private final TenderService tenderService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final TenderHistoryService historyService;
    private final NotificationService notificationService;
    private final DocumentVersionService documentVersionService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public TenderDtos.DocumentResponse upload(Long tenderId, DocumentType type, MultipartFile file, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Fichier PDF obligatoire");
        }
        String original = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new InvalidUploadException("Seuls les fichiers PDF sont acceptes");
        }
        assertPdfSignature(file);
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path directory = root.resolve(Path.of("tenders", tenderId.toString())).normalize();
            if (!directory.startsWith(root)) {
                throw new InvalidUploadException("Chemin de stockage invalide");
            }
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + ".pdf";
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) {
                throw new InvalidUploadException("Chemin de stockage invalide");
            }
            User uploadedBy = userRepository.findById(principal.getId()).orElse(null);
            TenderDocument document = new TenderDocument();
            document.setTender(tender);
            document.setFileName(storedName);
            document.setOriginalFileName(original);
            document.setFilePath(target.toString());
            document.setFileSize(file.getSize());
            document.setDocumentType(type == null ? DocumentType.RC : type);
            document.setUploadedBy(uploadedBy);
            TenderDocument saved = documentRepository.save(document);
            DocumentVersion version = documentVersionService.storeVersion(saved, 1, file, "Version initiale", uploadedBy);

            notificationService.create(tender.getAssignedEmployee(), tender, NotificationType.DOCUMENT_ADDED, "Document ajoute", "Nouveau document " + saved.getDocumentType() + " ajoute pour " + tender.getReference() + ".");
            historyService.record(tender, uploadedBy, TenderHistoryAction.DOCUMENT_ADDED, "document", null, saved.getOriginalFileName(), (uploadedBy == null ? "Un utilisateur" : uploadedBy.getFirstName()) + " a ajoute le document " + saved.getOriginalFileName() + ".");
            saved.setFileName(version.getStoredFileName());
            saved.setFilePath(version.getFilePath());
            saved.setFileSize(version.getFileSize());
            return mapperService.toDocumentResponse(saved);
        } catch (IOException ex) {
            throw new InvalidUploadException("Upload impossible");
        }
    }

    @Transactional(readOnly = true)
    public List<TenderDtos.DocumentResponse> list(Long tenderId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        return documentRepository.findByTenderIdOrderByUploadedAtDesc(tenderId).stream().map(mapperService::toDocumentResponse).toList();
    }

    @Transactional(readOnly = true)
    public DownloadedDocument download(Long id, UserPrincipal principal) {
        TenderDocument document = securedDocument(id, principal);
        try {
            Resource resource = new UrlResource(Path.of(document.getFilePath()).normalize().toUri());
            if (!resource.exists()) {
                throw new NotFoundException("Fichier introuvable");
            }
            return new DownloadedDocument(resource, document.getOriginalFileName());
        } catch (MalformedURLException ex) {
            throw new NotFoundException("Fichier introuvable");
        }
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        TenderDocument document = securedDocument(id, principal);
        if (isEmployee(principal)) {
            throw new ForbiddenOperationException("Suppression reservee a l'administrateur");
        }
        User actor = userRepository.findById(principal.getId()).orElse(null);
        historyService.record(document.getTender(), actor, TenderHistoryAction.DOCUMENT_DELETED, "document", document.getOriginalFileName(), null, (actor == null ? "Un utilisateur" : actor.getFirstName()) + " a supprime le document " + document.getOriginalFileName() + ".");
        documentRepository.delete(document);
        try {
            Files.deleteIfExists(Path.of(document.getFilePath()).normalize());
        } catch (IOException ignored) {
        }
    }

    private TenderDocument securedDocument(Long id, UserPrincipal principal) {
        TenderDocument document = documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document introuvable"));
        tenderService.findSecuredEntity(document.getTender().getId(), principal);
        return document;
    }

    private boolean isEmployee(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    private void assertPdfSignature(MultipartFile file) {
        try {
            String header = new String(file.getInputStream().readNBytes(5));
            if (!header.startsWith("%PDF-")) {
                throw new InvalidUploadException("Le fichier PDF est invalide");
            }
        } catch (IOException ex) {
            throw new InvalidUploadException("Le fichier PDF est invalide");
        }
    }

    public record DownloadedDocument(Resource resource, String originalFileName) {}
}
