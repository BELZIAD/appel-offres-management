package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/api/tenders/{id}/favorite")
    public TenderDtos.TenderResponse add(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return favoriteService.add(id, principal);
    }

    @DeleteMapping("/api/tenders/{id}/favorite")
    public void remove(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        favoriteService.remove(id, principal);
    }

    @GetMapping("/api/favorites")
    public List<TenderDtos.TenderResponse> favorites(@AuthenticationPrincipal UserPrincipal principal) {
        return favoriteService.list(principal);
    }
}
