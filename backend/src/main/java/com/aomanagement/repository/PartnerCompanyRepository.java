package com.aomanagement.repository;

import com.aomanagement.entity.PartnerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerCompanyRepository extends JpaRepository<PartnerCompany, Long> {
    List<PartnerCompany> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    List<PartnerCompany> findAllByOrderByNameAsc();
}
