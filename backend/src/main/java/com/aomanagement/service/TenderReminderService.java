package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.*;
import com.aomanagement.repository.EmailReminderRepository;
import com.aomanagement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TenderReminderService {
    private static final Logger log = LoggerFactory.getLogger(TenderReminderService.class);

    private final TenderRepository tenderRepository;
    private final EmailReminderRepository reminderRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TenderHistoryService historyService;
    private final MapperService mapperService;
    private final TenderAssignmentService assignmentService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void runDailyDeadlineReminders() {
        processDeadlineTwoDays(LocalDate.now());
    }

    @Transactional
    public int processDeadlineTwoDays(LocalDate today) {
        LocalDate targetDeadline = today.plusDays(2);
        int processed = 0;
        for (Tender tender : tenderRepository.findByDeadlineAndStatusNot(targetDeadline, TenderStatus.CLOSED)) {
            java.util.LinkedHashMap<Long, User> recipients = new java.util.LinkedHashMap<>();
            if (tender.getAssignedEmployee() != null) {
                recipients.put(tender.getAssignedEmployee().getId(), tender.getAssignedEmployee());
            }
            for (TenderAssignment assignment : assignmentService.tenderAssignments(tender.getId())) {
                if (assignment.getAssignmentRole() == TenderAssignmentRole.TENDER_MANAGER || assignment.getAssignmentRole() == TenderAssignmentRole.SUBMISSION_MANAGER) {
                    recipients.put(assignment.getEmployee().getId(), assignment.getEmployee());
                }
            }
            for (User employee : recipients.values()) {
                if (!reminderRepository.existsByTenderIdAndEmployeeIdAndReminderType(tender.getId(), employee.getId(), ReminderType.DEADLINE_TWO_DAYS)) {
                    sendTwoDayReminder(tender, employee, today);
                    processed++;
                }
            }
        }
        return processed;
    }

    private void sendTwoDayReminder(Tender tender, User employee, LocalDate scheduledDate) {
        EmailReminder reminder = new EmailReminder();
        reminder.setTender(tender);
        reminder.setEmployee(employee);
        reminder.setReminderType(ReminderType.DEADLINE_TWO_DAYS);
        reminder.setScheduledDate(scheduledDate);
        reminder.setStatus(EmailReminderStatus.PENDING);
        reminderRepository.save(reminder);

        notificationService.create(employee, tender, NotificationType.DEADLINE_WARNING, "Echéance dans 2 jours", tender.getReference() + " expire dans 2 jours.");

        try {
            emailService.send(employee.getEmail(), "Rappel - Appel d'offres " + tender.getReference() + " arrive à échéance", emailBody(tender, employee));
            reminder.setStatus(EmailReminderStatus.SENT);
            reminder.setSentAt(LocalDateTime.now());
            historyService.record(tender, null, TenderHistoryAction.UPDATED, null, null, null, "Le système a envoyé le rappel J-2.");
        } catch (RuntimeException ex) {
            log.warn("Deadline reminder email failed for tender {}", tender.getId(), ex);
            reminder.setStatus(EmailReminderStatus.FAILED);
            reminder.setErrorMessage(ex.getMessage());
            historyService.record(tender, null, TenderHistoryAction.UPDATED, null, null, null, "Le système n'a pas pu envoyer le rappel J-2.");
        }
    }

    @Transactional(readOnly = true)
    public Page<TenderDtos.EmailReminderResponse> search(EmailReminderStatus status, Long tenderId, Long employeeId, Pageable pageable) {
        Specification<EmailReminder> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (tenderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tender").get("id"), tenderId));
        }
        if (employeeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        return reminderRepository.findAll(spec, pageable).map(mapperService::toEmailReminderResponse);
    }

    private String emailBody(Tender tender, User employee) {
        return """
                Bonjour %s,

                L'appel d'offres :

                %s
                %s
                %s

                arrive à échéance le :

                %s

                Il reste 2 jours pour finaliser le dossier.

                Merci de vérifier l'état d'avancement du marché.
                """.formatted(employee.getFirstName(), tender.getReference(), tender.getTitle(), tender.getOrganization(), tender.getDeadline());
    }
}
