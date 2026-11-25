package com.aomanagement.repository;

import com.aomanagement.entity.TenderCompanyParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderCompanyParticipationRepository extends JpaRepository<TenderCompanyParticipation, Long> {
    List<TenderCompanyParticipation> findByTenderIdOrderByLeadCompanyDescIdAsc(Long tenderId);
    long countByPartnerCompanyId(Long partnerCompanyId);
    boolean existsByTenderIdAndLeadCompanyTrue(Long tenderId);
    boolean existsByTenderIdAndLeadCompanyTrueAndIdNot(Long tenderId, Long id);
}
