package com.aomanagement.repository;

import com.aomanagement.entity.PricingStatus;
import com.aomanagement.entity.TenderPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenderPricingRepository extends JpaRepository<TenderPricing, Long> {
    Optional<TenderPricing> findByTenderId(Long tenderId);
    List<TenderPricing> findByAssignedPricingManagerIdOrderByTenderDeadlineAsc(Long employeeId);
    long countByStatus(PricingStatus status);
}
