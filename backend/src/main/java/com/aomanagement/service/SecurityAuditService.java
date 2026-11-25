package com.aomanagement.service;

import com.aomanagement.dto.SecurityDtos;
import com.aomanagement.entity.SecurityAudit;
import com.aomanagement.entity.User;
import com.aomanagement.repository.SecurityAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {
    private final SecurityAuditRepository repository;

    public void record(User user, String email, String ip, String action, String reason, String userAgent) {
        SecurityAudit audit = new SecurityAudit();
        audit.setUser(user);
        audit.setEmail(clean(email));
        audit.setIpAddress(clean(ip));
        audit.setAction(action);
        audit.setReason(clean(reason));
        audit.setUserAgent(trim(userAgent));
        repository.save(audit);
    }

    public Page<SecurityDtos.SecurityAuditResponse> search(String email, Pageable pageable) {
        Page<SecurityAudit> page = email == null || email.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(email, pageable);
        return page.map(this::toResponse);
    }

    private SecurityDtos.SecurityAuditResponse toResponse(SecurityAudit audit) {
        return new SecurityDtos.SecurityAuditResponse(
                audit.getId(),
                audit.getUser() == null ? null : audit.getUser().getId(),
                audit.getEmail(),
                audit.getIpAddress(),
                audit.getAction(),
                audit.getReason(),
                audit.getUserAgent(),
                audit.getCreatedAt()
        );
    }

    private String trim(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.substring(0, Math.min(cleaned.length(), 500));
    }

    private String clean(String value) {
        return value == null ? null : value.replace('\n', ' ').replace('\r', ' ');
    }
}
