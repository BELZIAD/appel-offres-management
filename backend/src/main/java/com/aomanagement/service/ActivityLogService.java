package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.ActivityLog;
import com.aomanagement.entity.Tender;
import com.aomanagement.entity.User;
import com.aomanagement.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final MapperService mapperService;

    @Transactional
    public void record(User user, Tender tender, String entityType, Long entityId, String action, String description) {
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setTender(tender);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDescription(description);
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<SprintFourDtos.ActivityResponse> search(Long userId, Long tenderId, String action, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<ActivityLog> spec = Specification.where(null);
        if (userId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        if (tenderId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("tender").get("id"), tenderId));
        if (action != null && !action.isBlank()) spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        if (dateFrom != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
        if (dateTo != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
        return activityLogRepository.findAll(spec, pageable).map(mapperService::toActivityResponse);
    }
}
