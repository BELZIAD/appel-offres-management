package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.FavoriteTender;
import com.aomanagement.entity.Tender;
import com.aomanagement.entity.User;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.FavoriteTenderRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteTenderRepository favoriteRepository;
    private final TenderService tenderService;
    private final UserRepository userRepository;
    private final MapperService mapperService;

    @Transactional
    public TenderDtos.TenderResponse add(Long tenderId, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        if (!favoriteRepository.existsByUserIdAndTenderId(principal.getId(), tenderId)) {
            User user = userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
            FavoriteTender favorite = new FavoriteTender();
            favorite.setUser(user);
            favorite.setTender(tender);
            favoriteRepository.save(favorite);
        }
        return mapperService.toTenderResponse(tender, false, principal.getId());
    }

    @Transactional
    public void remove(Long tenderId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        favoriteRepository.findByUserIdAndTenderId(principal.getId(), tenderId).ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<TenderDtos.TenderResponse> list(UserPrincipal principal) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(f -> mapperService.toTenderResponse(f.getTender(), false, principal.getId()))
                .toList();
    }
}
