package com.aomanagement.service;

import com.aomanagement.dto.SettingsDtos;
import com.aomanagement.entity.BackupHistory;
import com.aomanagement.entity.BackupStatus;
import com.aomanagement.repository.BackupHistoryRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupService {
    private final BackupHistoryRepository backupHistoryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.backup.dir}")
    private String backupDir;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    public SettingsDtos.BackupListResponse list() {
        List<SettingsDtos.BackupItem> items = backupHistoryRepository.findAllByOrderByStartedAtDesc().stream()
                .map(this::toItem)
                .toList();
        return new SettingsDtos.BackupListResponse(items);
    }

    @Transactional
    public SettingsDtos.BackupItem create(UserPrincipal principal) {
        if (backupHistoryRepository.existsByStatus(BackupStatus.RUNNING)) {
            throw new IllegalStateException("Une sauvegarde est deja en cours");
        }
        BackupHistory history = new BackupHistory();
        history.setStatus(BackupStatus.RUNNING);
        history.setStartedAt(LocalDateTime.now());
        history.setBackupType("POSTGRES_CUSTOM");
        history.setCreatedBy(userRepository.findById(principal.getId()).orElse(null));
        history.setFileName(fileName());
        Path target = target(history.getFileName());
        history.setFilePath(target.toString());
        backupHistoryRepository.save(history);

        try {
            Files.createDirectories(target.getParent());
            if (datasourceUrl.startsWith("jdbc:h2:")) {
                history.setBackupType("H2_SCRIPT_TEST");
                jdbcTemplate.execute("SCRIPT TO '" + target.toString().replace("\\", "\\\\").replace("'", "''") + "'");
            } else {
                DatabaseTarget db = parseDatasource();
                ProcessBuilder dump = new ProcessBuilder(
                        "pg_dump",
                        "-h", db.host(),
                        "-p", db.port(),
                        "-U", datasourceUsername,
                        "-d", db.database(),
                        "-F", "c",
                        "-f", target.toString()
                );
                dump.environment().put("PGPASSWORD", datasourcePassword);
                int dumpExit = dump.inheritIO().start().waitFor();
                if (dumpExit != 0) {
                    throw new IllegalStateException("pg_dump a echoue");
                }
            }
            if (!Files.exists(target) || Files.size(target) == 0) throw new IllegalStateException("backup vide");
            history.setFileSize(Files.size(target));
            history.setChecksumSha256(checksum(target));
            if (!datasourceUrl.startsWith("jdbc:h2:")) {
                verifyWithPgRestore(target);
            }
            history.setStatus(BackupStatus.VERIFIED);
            history.setCompletedAt(LocalDateTime.now());
            return toItem(history);
        } catch (Exception ex) {
            history.setStatus(BackupStatus.FAILED);
            history.setErrorMessage(clean(ex.getMessage()));
            history.setCompletedAt(LocalDateTime.now());
            throw new IllegalStateException("Sauvegarde PostgreSQL impossible: " + clean(ex.getMessage()), ex);
        }
    }

    public Resource download(String name) {
        try {
            BackupHistory history = backupHistoryRepository.findByFileName(name).orElseThrow();
            Path target = Path.of(history.getFilePath()).toAbsolutePath().normalize();
            if (!target.startsWith(root()) || !Files.exists(target)) throw new IllegalArgumentException("Backup introuvable");
            return new UrlResource(target.toUri());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Backup introuvable");
        }
    }

    @Transactional
    public void delete(String name) {
        backupHistoryRepository.findByFileName(name).ifPresent(history -> {
            try {
                Path target = Path.of(history.getFilePath()).toAbsolutePath().normalize();
                if (target.startsWith(root())) Files.deleteIfExists(target);
            } catch (Exception ignored) {
            }
            backupHistoryRepository.delete(history);
        });
    }

    private void verifyWithPgRestore(Path target) throws Exception {
        ProcessBuilder restore = new ProcessBuilder("pg_restore", "--list", target.toString());
        int restoreExit = restore.inheritIO().start().waitFor();
        if (restoreExit != 0) {
            throw new IllegalStateException("pg_restore --list a echoue");
        }
    }

    private SettingsDtos.BackupItem toItem(BackupHistory history) {
        return new SettingsDtos.BackupItem(history.getFileName(), history.getFileSize(), history.getStartedAt(), history.getStatus().name(), history.getChecksumSha256());
    }

    private String fileName() {
        return "ao_" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").format(LocalDateTime.now()) + ".dump";
    }

    private Path target(String fileName) {
        LocalDate today = LocalDate.now();
        return root().resolve(String.format("%04d/%02d", today.getYear(), today.getMonthValue())).resolve(fileName).normalize();
    }

    private Path root() {
        return Path.of(backupDir).toAbsolutePath().normalize();
    }

    private String checksum(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private DatabaseTarget parseDatasource() {
        String value = datasourceUrl.replace("jdbc:postgresql://", "");
        String hostPort = value.substring(0, value.indexOf('/'));
        String database = value.substring(value.indexOf('/') + 1).split("\\?")[0];
        String[] parts = hostPort.split(":");
        return new DatabaseTarget(parts[0], parts.length > 1 ? parts[1] : "5432", database);
    }

    private String clean(String value) {
        if (value == null) return "Erreur inconnue";
        return value.replace('\n', ' ').replace('\r', ' ').substring(0, Math.min(value.length(), 500));
    }

    private record DatabaseTarget(String host, String port, String database) {}
}
