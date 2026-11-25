package com.aomanagement.dto;

import com.aomanagement.dto.TenderDtos.TenderResponse;

import java.math.BigDecimal;
import java.util.List;

public class StatisticsDtos {
    public record StatisticsFilter(java.time.LocalDate dateFrom, java.time.LocalDate dateTo) {}

    public record OverviewResponse(
            long totalTenders,
            long inProgress,
            long submitted,
            long won,
            long lost,
            long pending,
            long cancelled,
            BigDecimal winRate,
            BigDecimal totalEstimatedAmount,
            BigDecimal totalWonAmount,
            BigDecimal averageWonAmount,
            long urgent,
            long overdue
    ) {}

    public record MonthlyResponse(int month, long total, long won, long lost, long pending) {}

    public record YearlyResponse(int year, long total, long won, long lost, BigDecimal winRate, BigDecimal wonAmount) {}

    public record EmployeePerformanceResponse(
            Long employeeId,
            String employeeName,
            String email,
            boolean enabled,
            long assigned,
            long inProgress,
            long submitted,
            long won,
            long lost,
            long pending,
            BigDecimal winRate,
            BigDecimal wonAmount
    ) {}

    public record EmployeeDetailResponse(
            EmployeePerformanceResponse employee,
            long closed,
            List<TenderResponse> tenders,
            List<MonthlyResponse> monthly
    ) {}

    public record OrganizationResponse(
            String organization,
            long total,
            long won,
            long lost,
            long pending,
            BigDecimal winRate,
            BigDecimal estimatedAmount,
            BigDecimal wonAmount
    ) {}

    public record ResultsResponse(long won, long lost, long pending, long cancelled) {}
}
