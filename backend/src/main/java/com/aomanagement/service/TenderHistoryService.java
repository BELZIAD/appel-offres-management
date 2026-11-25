package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Tender;
import com.aomanagement.entity.TenderHistory;
import com.aomanagement.entity.TenderHistoryAction;
import com.aomanagement.entity.User;
import com.aomanagement.repository.TenderHistoryRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TenderHistoryService {
    private final TenderHistoryRepository historyRepository;
    private final MapperService mapperService;

    @Transactional
    public void record(Tender tender, User performedBy, TenderHistoryAction action, String fieldName, String oldValue, String newValue, String description) {
        TenderHistory history = new TenderHistory();
        history.setTender(tender);
        history.setPerformedBy(performedBy);
        history.setAction(action);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setDescription(description);
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<TenderDtos.HistoryResponse> forTender(Long tenderId, Pageable pageable) {
        return historyRepository.findByTenderIdOrderByCreatedAtDesc(tenderId, pageable).map(mapperService::toHistoryResponse);
    }

    @Transactional(readOnly = true)
    public Page<TenderDtos.HistoryResponse> all(Pageable pageable) {
        return historyRepository.findAllByOrderByCreatedAtDesc(pageable).map(mapperService::toHistoryResponse);
    }

    @Transactional(readOnly = true)
    public Page<TenderDtos.HistoryResponse> search(
            TenderHistoryAction action,
            Long userId,
            Long tenderId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            Pageable pageable
    ) {
        Specification<TenderHistory> spec = (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("tender", JoinType.LEFT);
                root.fetch("performedBy", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("performedBy").get("id"), userId));
        }
        if (tenderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tender").get("id"), tenderId));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
        }
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("description")), term),
                    cb.like(cb.lower(root.get("fieldName")), term),
                    cb.like(cb.lower(root.join("tender", JoinType.LEFT).get("reference")), term),
                    cb.like(cb.lower(root.join("tender", JoinType.LEFT).get("title")), term),
                    cb.like(cb.lower(root.join("performedBy", JoinType.LEFT).get("firstName")), term),
                    cb.like(cb.lower(root.join("performedBy", JoinType.LEFT).get("lastName")), term),
                    cb.like(cb.lower(root.join("performedBy", JoinType.LEFT).get("email")), term)
            ));
        }
        return historyRepository.findAll(spec, pageable).map(mapperService::toHistoryResponse);
    }
}
