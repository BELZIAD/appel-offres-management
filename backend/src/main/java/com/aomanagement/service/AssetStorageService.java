package com.aomanagement.service;

import com.aomanagement.exception.InvalidUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetStorageService {
    @Value("${app.upload-dir}") private String uploadDir;
    @Value("${app.branding.max-file-size-bytes:2097152}") private long maxFileSize;

    private static final Set<String> ALLOWED = Set.of("png", "svg", "ico", "jpg", "jpeg");

    public String store(String folder, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidUploadException("Fichier obligatoire");
        if (file.getSize() > maxFileSize) throw new InvalidUploadException("Fichier trop volumineux");
        String original = file.getOriginalFilename() == null ? "asset" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) throw new InvalidUploadException("Format image non autorise");
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path dir = root.resolve(Path.of(folder)).normalize();
            if (!dir.startsWith(root)) throw new InvalidUploadException("Chemin invalide");
            Files.createDirectories(dir);
            String stored = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(stored).normalize();
            if (!target.startsWith(dir)) throw new InvalidUploadException("Chemin invalide");
            file.transferTo(target);
            return "/api/assets/" + folder.replace("\\", "/") + "/" + stored;
        } catch (IOException ex) {
            throw new InvalidUploadException("Upload impossible");
        }
    }

    public Resource load(String folder, String name) {
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path target = root.resolve(Path.of(folder, name)).normalize();
            if (!target.startsWith(root) || !Files.exists(target)) throw new InvalidUploadException("Asset introuvable");
            return new UrlResource(target.toUri());
        } catch (Exception ex) {
            throw new InvalidUploadException("Asset introuvable");
        }
    }

    public void delete(String url) {
        if (url == null || url.isBlank() || !url.startsWith("/api/assets/")) return;
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path relative = Path.of(url.substring("/api/assets/".length())).normalize();
            Path target = root.resolve(relative).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // The database value is still cleared even if the old file is already absent.
        }
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
