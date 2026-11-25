package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/api/tenders/{id}/comments")
    public List<SprintFourDtos.CommentResponse> list(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.list(id, principal);
    }

    @PostMapping("/api/tenders/{id}/comments")
    public SprintFourDtos.CommentResponse create(@PathVariable Long id, @Valid @RequestBody SprintFourDtos.CommentRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.create(id, request, principal);
    }

    @PutMapping("/api/tenders/{tenderId}/comments/{commentId}")
    public SprintFourDtos.CommentResponse update(@PathVariable Long tenderId, @PathVariable Long commentId, @Valid @RequestBody SprintFourDtos.CommentRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.update(tenderId, commentId, request, principal);
    }

    @DeleteMapping("/api/tenders/{tenderId}/comments/{commentId}")
    public void delete(@PathVariable Long tenderId, @PathVariable Long commentId, @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(tenderId, commentId, principal);
    }
}
