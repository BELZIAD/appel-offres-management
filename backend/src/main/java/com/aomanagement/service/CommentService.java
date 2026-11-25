package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderCommentRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final TenderCommentRepository commentRepository;
    private final TenderService tenderService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<SprintFourDtos.CommentResponse> list(Long tenderId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        return commentRepository.findByTenderIdOrderByCreatedAtAsc(tenderId).stream().map(mapperService::toCommentResponse).toList();
    }

    @Transactional
    public SprintFourDtos.CommentResponse create(Long tenderId, SprintFourDtos.CommentRequest request, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        User author = currentUser(principal);
        TenderComment comment = new TenderComment();
        comment.setTender(tender);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        TenderComment saved = commentRepository.save(comment);
        notifyComment(tender, author, request.content());
        activityLogService.record(author, tender, "COMMENT", saved.getId(), "TENDER_COMMENT_ADDED", author.getFirstName() + " a ajoute un commentaire.");
        return mapperService.toCommentResponse(saved);
    }

    @Transactional
    public SprintFourDtos.CommentResponse update(Long tenderId, Long commentId, SprintFourDtos.CommentRequest request, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        TenderComment comment = findForTender(tenderId, commentId);
        User actor = currentUser(principal);
        if (!isAdmin(principal) && (comment.getAuthor() == null || !comment.getAuthor().getId().equals(actor.getId()))) {
            throw new ForbiddenOperationException("Modification interdite");
        }
        comment.setContent(request.content().trim());
        activityLogService.record(actor, comment.getTender(), "COMMENT", comment.getId(), "TENDER_COMMENT_UPDATED", actor.getFirstName() + " a modifie un commentaire.");
        return mapperService.toCommentResponse(comment);
    }

    @Transactional
    public void delete(Long tenderId, Long commentId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        TenderComment comment = findForTender(tenderId, commentId);
        User actor = currentUser(principal);
        if (!isAdmin(principal) && (comment.getAuthor() == null || !comment.getAuthor().getId().equals(actor.getId()))) {
            throw new ForbiddenOperationException("Suppression interdite");
        }
        comment.setDeleted(true);
        activityLogService.record(actor, comment.getTender(), "COMMENT", comment.getId(), "TENDER_COMMENT_DELETED", actor.getFirstName() + " a supprime un commentaire.");
    }

    private void notifyComment(Tender tender, User author, String content) {
        Set<Long> notified = new HashSet<>();
        if (author.getRole() == Role.ADMIN) {
            User employee = tender.getAssignedEmployee();
            if (employee != null && !employee.getId().equals(author.getId())) {
                notificationService.create(employee, tender, NotificationType.TENDER_COMMENT_ADDED, "Nouveau commentaire", "Nouveau commentaire sur " + tender.getReference() + ".");
                notified.add(employee.getId());
            }
        } else {
            for (User admin : userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Role.ADMIN)) {
                notificationService.create(admin, tender, NotificationType.TENDER_COMMENT_ADDED, "Nouveau commentaire", "Nouveau commentaire sur " + tender.getReference() + ".");
                notified.add(admin.getId());
            }
        }
        String lower = content.toLowerCase(Locale.ROOT);
        for (User user : userRepository.findAll()) {
            if (user.getId().equals(author.getId()) || notified.contains(user.getId())) continue;
            if (lower.contains("@" + user.getEmail().toLowerCase(Locale.ROOT))
                    || lower.contains("@" + user.getFirstName().toLowerCase(Locale.ROOT))
                    || lower.contains("@" + user.getLastName().toLowerCase(Locale.ROOT))) {
                notificationService.create(user, tender, NotificationType.USER_MENTIONED, "Mention", "Vous avez ete mentionne dans un commentaire sur " + tender.getReference() + ".");
                notified.add(user.getId());
            }
        }
    }

    private TenderComment findForTender(Long tenderId, Long commentId) {
        TenderComment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Commentaire introuvable"));
        if (!comment.getTender().getId().equals(tenderId)) {
            throw new NotFoundException("Commentaire introuvable");
        }
        return comment;
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
