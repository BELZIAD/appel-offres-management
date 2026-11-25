package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.TaskPriority;
import com.aomanagement.entity.TaskStatus;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/api/admin/tasks")
    public Page<SprintFourDtos.TaskResponse> adminTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long tenderId,
            @RequestParam(required = false) LocalDate deadlineFrom,
            @RequestParam(required = false) LocalDate deadlineTo,
            Pageable pageable
    ) {
        return taskService.adminSearch(search, status, priority, employeeId, tenderId, deadlineFrom, deadlineTo, pageable);
    }

    @GetMapping("/api/admin/tasks/{id}")
    public SprintFourDtos.TaskResponse adminTask(@PathVariable Long id) {
        return taskService.getAdmin(id);
    }

    @PostMapping("/api/admin/tasks")
    public SprintFourDtos.TaskResponse create(@Valid @RequestBody SprintFourDtos.TaskRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.create(request, principal);
    }

    @PutMapping("/api/admin/tasks/{id}")
    public SprintFourDtos.TaskResponse update(@PathVariable Long id, @Valid @RequestBody SprintFourDtos.TaskRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.update(id, request, principal);
    }

    @DeleteMapping("/api/admin/tasks/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        taskService.delete(id, principal);
    }

    @PatchMapping("/api/admin/tasks/{id}/assign/{employeeId}")
    public SprintFourDtos.TaskResponse assign(@PathVariable Long id, @PathVariable Long employeeId, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.assign(id, employeeId, principal);
    }

    @PatchMapping("/api/admin/tasks/{id}/status")
    public SprintFourDtos.TaskResponse adminStatus(@PathVariable Long id, @Valid @RequestBody SprintFourDtos.TaskStatusRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.updateAdminStatus(id, request.status(), principal);
    }

    @GetMapping("/api/employee/tasks")
    public Page<SprintFourDtos.TaskResponse> employeeTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) LocalDate deadlineFrom,
            @RequestParam(required = false) LocalDate deadlineTo,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return taskService.employeeSearch(principal.getId(), search, status, priority, deadlineFrom, deadlineTo, pageable);
    }

    @GetMapping("/api/employee/tasks/{id}")
    public SprintFourDtos.TaskResponse employeeTask(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.getEmployee(id, principal);
    }

    @PatchMapping("/api/employee/tasks/{id}/status")
    public SprintFourDtos.TaskResponse employeeStatus(@PathVariable Long id, @Valid @RequestBody SprintFourDtos.TaskStatusRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.updateEmployeeStatus(id, request.status(), principal);
    }
}
