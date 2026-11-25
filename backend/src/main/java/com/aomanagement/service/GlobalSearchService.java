package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Role;
import com.aomanagement.repository.TenderDocumentRepository;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.TenderTaskRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {
    private final TenderRepository tenderRepository;
    private final TenderTaskRepository taskRepository;
    private final TenderDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MapperService mapperService;

    @Transactional(readOnly = true)
    public SprintFourDtos.SearchResponse search(String q, UserPrincipal principal) {
        String term = q == null ? "" : q.trim().toLowerCase();
        if (term.length() < 2) {
            return new SprintFourDtos.SearchResponse(List.of(), List.of(), List.of(), List.of());
        }
        String like = "%" + term + "%";
        boolean admin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Specification<com.aomanagement.entity.Tender> tenderSpec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("reference")), like),
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("organization")), like)
        );
        if (!admin) {
            tenderSpec = tenderSpec.and((root, query, cb) -> cb.equal(root.get("assignedEmployee").get("id"), principal.getId()));
        }
        Specification<com.aomanagement.entity.TenderTask> taskSpec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("tender").get("reference")), like)
        );
        if (!admin) {
            taskSpec = taskSpec.and((root, query, cb) -> cb.equal(root.get("assignedEmployee").get("id"), principal.getId()));
        }
        Specification<com.aomanagement.entity.TenderDocument> documentSpec = (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("originalFileName")), like),
                cb.like(cb.lower(root.get("tender").get("reference")), like)
        );
        if (!admin) {
            documentSpec = documentSpec.and((root, query, cb) -> cb.equal(root.get("tender").get("assignedEmployee").get("id"), principal.getId()));
        }
        List<TenderDtos.TenderResponse> tenders = tenderRepository.findAll(tenderSpec, PageRequest.of(0, 5)).map(t -> mapperService.toTenderResponse(t, false, principal.getId())).toList();
        List<SprintFourDtos.TaskResponse> tasks = taskRepository.findAll(taskSpec, PageRequest.of(0, 5)).map(mapperService::toTaskResponse).toList();
        List<TenderDtos.DocumentResponse> documents = documentRepository.findAll(documentSpec, PageRequest.of(0, 5)).map(mapperService::toDocumentResponse).toList();
        List<TenderDtos.SimpleUser> users = admin ? userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Role.EMPLOYEE).stream()
                .filter(u -> (u.getFirstName() + " " + u.getLastName() + " " + u.getEmail()).toLowerCase().contains(term))
                .limit(5)
                .map(mapperService::toSimpleUser)
                .toList() : List.of();
        return new SprintFourDtos.SearchResponse(tenders, tasks, documents, users);
    }
}
