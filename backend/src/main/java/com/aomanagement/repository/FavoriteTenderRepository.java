package com.aomanagement.repository;

import com.aomanagement.entity.FavoriteTender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteTenderRepository extends JpaRepository<FavoriteTender, Long> {
    boolean existsByUserIdAndTenderId(Long userId, Long tenderId);
    Optional<FavoriteTender> findByUserIdAndTenderId(Long userId, Long tenderId);
    List<FavoriteTender> findByUserIdOrderByCreatedAtDesc(Long userId);
}
