package com.aomanagement.service;

import com.aomanagement.dto.UserDtos;
import com.aomanagement.entity.Role;
import com.aomanagement.entity.User;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final MapperService mapperService;

    public List<UserDtos.EmployeeResponse> employees() {
        return userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Role.EMPLOYEE).stream()
                .map(mapperService::toEmployeeResponse)
                .toList();
    }

    public UserDtos.EmployeeResponse employee(Long id) {
        return mapperService.toEmployeeResponse(findEmployee(id));
    }

    @Transactional
    public UserDtos.EmployeeResponse create(UserDtos.EmployeeRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email déjà utilisé");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ConflictException("Le mot de passe est obligatoire");
        }
        passwordPolicyService.validate(request.password());
        User user = new User();
        apply(user, request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.EMPLOYEE);
        return mapperService.toEmployeeResponse(userRepository.save(user));
    }

    @Transactional
    public UserDtos.EmployeeResponse update(Long id, UserDtos.EmployeeRequest request) {
        User user = findEmployee(id);
        userRepository.findByEmailIgnoreCase(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Email déjà utilisé"); });
        apply(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicyService.validate(request.password());
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return mapperService.toEmployeeResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(findEmployee(id));
        try {
            userRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException("Cet employe est lie a des appels d'offres, taches ou documents. Desactivez son compte au lieu de le supprimer.");
        }
    }

    private void apply(User user, UserDtos.EmployeeRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setEnabled(request.enabled() == null || request.enabled());
    }

    /** Changement de mot de passe par l'utilisateur connecte (verification de l'ancien mot de passe). */
    @Transactional
    public void changeOwnPassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ConflictException("Mot de passe actuel incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ConflictException("Le nouveau mot de passe doit etre different de l'ancien");
        }
        passwordPolicyService.validate(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
    }

    public User findEmployee(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Employé introuvable"));
        if (user.getRole() != Role.EMPLOYEE) {
            throw new NotFoundException("Employé introuvable");
        }
        return user;
    }
}
