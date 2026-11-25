package com.aomanagement.repository;

import com.aomanagement.entity.BackupHistory;
import com.aomanagement.entity.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {
    List<BackupHistory> findAllByOrderByStartedAtDesc();
    Optional<BackupHistory> findByFileName(String fileName);
    boolean existsByStatus(BackupStatus status);
    Optional<BackupHistory> findFirstByStatusInOrderByStartedAtDesc(List<BackupStatus> statuses);
}
