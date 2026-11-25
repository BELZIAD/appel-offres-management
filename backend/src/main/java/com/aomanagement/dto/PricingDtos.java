package com.aomanagement.dto;

import com.aomanagement.entity.PricingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PricingDtos {
    public record PricingResponse(Long id, Long tenderId, PricingStatus status, TenderDtos.SimpleUser assignedPricingManager, BigDecimal estimatedTotal, BigDecimal finalTotal, String currency, String notes, LocalDateTime validatedAt, TenderDtos.SimpleUser validatedBy, List<PricingLineResponse> lines) {}
    public record PricingLineRequest(String lineNumber, String reference, @NotBlank String designation, String unit, BigDecimal quantity, BigDecimal unitPrice, String notes, Integer position) {}
    public record PricingLineResponse(Long id, String lineNumber, String reference, String designation, String unit, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice, String notes, int position) {}
    public record PricingStatusRequest(@NotNull PricingStatus status, String notes) {}
    public record PricingImportPreview(List<PricingLineRequest> lines, boolean containsPrices) {}
}
