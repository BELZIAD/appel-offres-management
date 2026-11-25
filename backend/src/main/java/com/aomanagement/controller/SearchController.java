package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {
    private final GlobalSearchService globalSearchService;

    @GetMapping("/api/search")
    public SprintFourDtos.SearchResponse search(@RequestParam String q, @AuthenticationPrincipal UserPrincipal principal) {
        return globalSearchService.search(q, principal);
    }
}
