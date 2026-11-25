package com.aomanagement.controller;

import com.aomanagement.dto.UserDtos;
import com.aomanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {
    private final UserService userService;

    @GetMapping
    public List<UserDtos.EmployeeResponse> all() {
        return userService.employees();
    }

    @GetMapping("/{id}")
    public UserDtos.EmployeeResponse one(@PathVariable Long id) {
        return userService.employee(id);
    }

    @PostMapping
    public UserDtos.EmployeeResponse create(@Valid @RequestBody UserDtos.EmployeeRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDtos.EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.EmployeeRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
