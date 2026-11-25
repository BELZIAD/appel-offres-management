package com.aomanagement.dto;

import com.aomanagement.entity.ParticipationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PartnerDtos {
    public record PartnerRequest(@NotBlank String name, String tradeName, String ice, String rc, String ifNumber, String address, String city, String country, String email, String phone, String contactPerson, Boolean active, String notes) {}
    public record PartnerResponse(Long id, String name, String tradeName, String ice, String rc, String ifNumber, String address, String city, String country, String email, String phone, String contactPerson, boolean active, String notes, long sharedTenderCount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record PartnerStatusRequest(boolean active) {}
    public record ParticipationRequest(Long partnerCompanyId, @NotNull ParticipationType participationType, boolean ourCompany, boolean leadCompany, BigDecimal sharePercentage, String roleDescription) {}
    public record ParticipationResponse(Long id, Long partnerCompanyId, String companyName, ParticipationType participationType, boolean ourCompany, boolean leadCompany, BigDecimal sharePercentage, String roleDescription) {}
    public record ParticipationListResponse(List<ParticipationResponse> participations) {}
}
