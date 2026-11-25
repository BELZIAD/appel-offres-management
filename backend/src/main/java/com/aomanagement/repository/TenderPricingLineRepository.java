package com.aomanagement.repository;

import com.aomanagement.entity.TenderPricingLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderPricingLineRepository extends JpaRepository<TenderPricingLine, Long> {
    List<TenderPricingLine> findByPricingIdOrderByPositionAscIdAsc(Long pricingId);
    void deleteByPricingId(Long pricingId);
}
