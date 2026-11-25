package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderTaskRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TenderTaskRepository taskRepository;
    private final TenderService tenderService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final NotificationService notificationService;
    private final TenderHistoryService historyService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public Page<SprintFourDtos.TaskResponse> adminSearch(String search, TaskStatus status, TaskPriority priority, Long employeeId, Long tenderId, LocalDate deadlineFrom, LocalDate deadlineTo, Pageable pageable) {
        return taskRepository.findAll(spec(search, status, priority, employeeId, tenderId, deadlineFrom, deadlineTo, null), pageable).map(mapperService::toTaskResponse);
    }

    @Transactional(readOnly = true)
    public Page<SprintFourDtos.TaskResponse> employeeSearch(Long employeeId, String search, TaskStatus status, TaskPriority priority, LocalDate deadlineFrom, LocalDate deadlineTo, Pageable pageable) {
        return taskRepository.findAll(spec(search, status, priority, employeeId, null, deadlineFrom, deadlineTo, employeeId), pageable).map(mapperService::toTaskResponse);
    }

    @Transactional(readOnly = true)
    public SprintFourDtos.TaskResponse getAdmin(Long id) {
        return mapperService.toTaskResponse(find(id));
    }

    @Transactional(readOnly = true)
    public SprintFourDtos.TaskResponse getEmployee(Long id, UserPrincipal principal) {
        TenderTask task = find(id);
        assertAssigned(task, principal);
        return mapperService.toTaskResponse(task);
    }

    @Transactional
    public SprintFourDtos.TaskResponse create(SprintFourDtos.TaskRequest request, UserPrincipal principal) {
        User actor = currentUser(principal);
        Tender tender = tenderService.find(request.tenderId());
        User employee = userService.findEmployee(request.assignedEmployeeId());
        TenderTask task = new TenderTask();
        apply(task, request, tender, employee);
        task.setCreatedBy(actor);
        setStatus(task, request.status() == null ? TaskStatus.TODO : request.status());
        TenderTask saved = taskRepository.save(task);
        notificationService.create(employee, tender, NotificationType.TASK_ASSIGNED, "Nouvelle tache", "Une nouvelle tache vous a ete affectee.");
        historyService.record(tender, actor, TenderHistoryAction.TASK_CREATED, "task", null, saved.getTitle(), actor.getFirstName() + " a cree la tache \"" + saved.getTitle() + "\".");
        activityLogService.record(actor, tender, "TASK", saved.getId(), "TASK_CREATED", actor.getFirstName() + " a cree la tache \"" + saved.getTitle() + "\".");
        return mapperService.toTaskResponse(saved);
    }

    @Transactional
    public SprintFourDtos.TaskResponse update(Long id, SprintFourDtos.TaskRequest request, UserPrincipal principal) {
        TenderTask task = find(id);
        User actor = currentUser(principal);
        User oldEmployee = task.getAssignedEmployee();
        TaskStatus oldStatus = task.getStatus();
        Tender tender = tenderService.find(request.tenderId());
        User employee = userService.findEmployee(request.assignedEmployeeId());
        apply(task, request, tender, employee);
        setStatus(task, request.status() == null ? task.getStatus() : request.status());
        if (oldEmployee == null || !oldEmployee.getId().equals(employee.getId())) {
            notificationService.create(employee, tender, NotificationType.TASK_ASSIGNED, "Nouvelle tache", "Une nouvelle tache vous a ete affectee.");
            historyService.record(tender, actor, oldEmployee == null ? TenderHistoryAction.TASK_ASSIGNED : TenderHistoryAction.TASK_REASSIGNED, "assignedEmployee", oldEmployee == null ? null : oldEmployee.getEmail(), employee.getEmail(), "Tache \"" + task.getTitle() + "\" affectee a " + employee.getFirstName() + ".");
        }
        if (oldStatus != task.getStatus()) {
            recordStatusChange(task, actor, oldStatus, task.getStatus());
        } else {
            historyService.record(tender, actor, TenderHistoryAction.TASK_UPDATED, "task", null, task.getTitle(), actor.getFirstName() + " a modifie la tache \"" + task.getTitle() + "\".");
        }
        activityLogService.record(actor, tender, "TASK", task.getId(), "TASK_UPDATED", actor.getFirstName() + " a modifie la tache \"" + task.getTitle() + "\".");
        return mapperService.toTaskResponse(task);
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        TenderTask task = find(id);
        User actor = currentUser(principal);
        historyService.record(task.getTender(), actor, TenderHistoryAction.TASK_DELETED, "task", task.getTitle(), null, actor.getFirstName() + " a supprime la tache \"" + task.getTitle() + "\".");
        activityLogService.record(actor, task.getTender(), "TASK", task.getId(), "TASK_DELETED", actor.getFirstName() + " a supprime une tache.");
        taskRepository.delete(task);
    }

    @Transactional
    public SprintFourDtos.TaskResponse assign(Long id, Long employeeId, UserPrincipal principal) {
        TenderTask task = find(id);
        User actor = currentUser(principal);
        User oldEmployee = task.getAssignedEmployee();
        User employee = userService.findEmployee(employeeId);
        task.setAssignedEmployee(employee);
        notificationService.create(employee, task.getTender(), NotificationType.TASK_ASSIGNED, "Nouvelle tache", "Une nouvelle tache vous a ete affectee.");
        historyService.record(task.getTender(), actor, oldEmployee == null ? TenderHistoryAction.TASK_ASSIGNED : TenderHistoryAction.TASK_REASSIGNED, "assignedEmployee", oldEmployee == null ? null : oldEmployee.getEmail(), employee.getEmail(), "Tache \"" + task.getTitle() + "\" affectee a " + employee.getFirstName() + ".");
        activityLogService.record(actor, task.getTender(), "TASK", task.getId(), "TASK_ASSIGNED", actor.getFirstName() + " a affecte la tache \"" + task.getTitle() + "\".");
        return mapperService.toTaskResponse(task);
    }

    @Transactional
    public SprintFourDtos.TaskResponse updateAdminStatus(Long id, TaskStatus status, UserPrincipal principal) {
        TenderTask task = find(id);
        User actor = currentUser(principal);
        TaskStatus old = task.getStatus();
        setStatus(task, status);
        recordStatusChange(task, actor, old, status);
        return mapperService.toTaskResponse(task);
    }

    @Transactional
    public SprintFourDtos.TaskResponse updateEmployeeStatus(Long id, TaskStatus status, UserPrincipal principal) {
        TenderTask task = find(id);
        assertAssigned(task, principal);
        User actor = currentUser(principal);
        TaskStatus old = task.getStatus();
        setStatus(task, status);
        recordStatusChange(task, actor, old, status);
        return mapperService.toTaskResponse(task);
    }

    @Transactional
    public void notifyTomorrowDeadlines() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<TenderTask> tasks = taskRepository.findByDeadlineAndStatusNot(tomorrow, TaskStatus.DONE);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        for (TenderTask task : tasks) {
            User employee = task.getAssignedEmployee();
            if (employee != null && !notificationService.existsSince(employee.getId(), task.getTender().getId(), NotificationType.TASK_DEADLINE_WARNING, "Tache a echeance demain", todayStart)) {
                notificationService.create(employee, task.getTender(), NotificationType.TASK_DEADLINE_WARNING, "Tache a echeance demain", "Une tache arrive a echeance demain.");
            }
        }
    }

    public TenderTask find(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Tache introuvable"));
    }

    private Specification<TenderTask> spec(String search, TaskStatus status, TaskPriority priority, Long employeeId, Long tenderId, LocalDate deadlineFrom, LocalDate deadlineTo, Long restrictEmployeeId) {
        Specification<TenderTask> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("description")), like), cb.like(cb.lower(root.get("tender").get("reference")), like)));
        }
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (priority != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        if (employeeId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedEmployee").get("id"), employeeId));
        if (tenderId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("tender").get("id"), tenderId));
        if (deadlineFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deadline"), deadlineFrom));
        if (deadlineTo != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("deadline"), deadlineTo));
        if (restrictEmployeeId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedEmployee").get("id"), restrictEmployeeId));
        return spec;
    }

    private void apply(TenderTask task, SprintFourDtos.TaskRequest request, Tender tender, User employee) {
        task.setTender(tender);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setAssignedEmployee(employee);
        task.setPriority(request.priority() == null ? TaskPriority.NORMAL : request.priority());
        task.setDeadline(request.deadline());
    }

    private void setStatus(TenderTask task, TaskStatus status) {
        task.setStatus(status);
        if (status == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }
        if (status != TaskStatus.DONE) {
            task.setCompletedAt(null);
        }
    }

    private void recordStatusChange(TenderTask task, User actor, TaskStatus oldStatus, TaskStatus newStatus) {
        if (oldStatus == newStatus) return;
        if (newStatus == TaskStatus.DONE) {
            notificationService.create(task.getCreatedBy(), task.getTender(), NotificationType.TASK_COMPLETED, "Tache terminee", "La tache \"" + task.getTitle() + "\" est terminee.");
        }
        if (newStatus == TaskStatus.BLOCKED) {
            notificationService.create(task.getCreatedBy(), task.getTender(), NotificationType.TASK_BLOCKED, "Tache bloquee", "La tache \"" + task.getTitle() + "\" est bloquee.");
        }
        historyService.record(task.getTender(), actor, TenderHistoryAction.TASK_STATUS_CHANGED, "task.status", oldStatus.name(), newStatus.name(), actor.getFirstName() + " a change le statut de la tache \"" + task.getTitle() + "\".");
        activityLogService.record(actor, task.getTender(), "TASK", task.getId(), "TASK_STATUS_CHANGED", actor.getFirstName() + " a passe la tache \"" + task.getTitle() + "\" a " + newStatus + ".");
    }

    private void assertAssigned(TenderTask task, UserPrincipal principal) {
        if (task.getAssignedEmployee() == null || !task.getAssignedEmployee().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException("Acces interdit a cette tache");
        }
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }
}
