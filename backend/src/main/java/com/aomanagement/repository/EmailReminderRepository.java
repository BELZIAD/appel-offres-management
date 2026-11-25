package com.aomanagement.repository;

import com.aomanagement.entity.EmailReminder;
import com.aomanagement.entity.EmailReminderStatus;
import com.aomanagement.entity.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmailReminderRepository extends JpaRepository<EmailReminder, Long>, JpaSpecificationExecutor<EmailReminder> {
    boolean existsByTenderIdAndReminderTypeAndStatus(Long tenderId, ReminderType reminderType, EmailReminderStatus status);
    boolean existsByTenderIdAndReminderType(Long tenderId, ReminderType reminderType);
    boolean existsByTenderIdAndEmployeeIdAndReminderType(Long tenderId, Long employeeId, ReminderType reminderType);
}
