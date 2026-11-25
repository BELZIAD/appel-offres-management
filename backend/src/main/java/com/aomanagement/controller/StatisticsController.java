package com.aomanagement.controller;

import com.aomanagement.dto.StatisticsDtos;
import com.aomanagement.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    public StatisticsDtos.OverviewResponse overview(@RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {
        return statisticsService.overview(new StatisticsDtos.StatisticsFilter(dateFrom, dateTo));
    }

    @GetMapping("/monthly")
    public List<StatisticsDtos.MonthlyResponse> monthly(@RequestParam(required = false) Integer year, @RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {
        return statisticsService.monthly(year == null ? LocalDate.now().getYear() : year, new StatisticsDtos.StatisticsFilter(dateFrom, dateTo));
    }

    @GetMapping("/yearly")
    public List<StatisticsDtos.YearlyResponse> yearly(@RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {
        return statisticsService.yearly(new StatisticsDtos.StatisticsFilter(dateFrom, dateTo));
    }

    @GetMapping("/employees")
    public List<StatisticsDtos.EmployeePerformanceResponse> employees(@RequestParam(required = false, defaultValue = "employeeName") String sort) {
        return statisticsService.employees(sort);
    }

    @GetMapping("/employees/{id}")
    public StatisticsDtos.EmployeeDetailResponse employee(@PathVariable Long id) {
        return statisticsService.employeeDetail(id);
    }

    @GetMapping("/organizations")
    public List<StatisticsDtos.OrganizationResponse> organizations(@RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {
        return statisticsService.organizations(new StatisticsDtos.StatisticsFilter(dateFrom, dateTo));
    }

    @GetMapping("/results")
    public StatisticsDtos.ResultsResponse results(@RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo) {
        return statisticsService.results(new StatisticsDtos.StatisticsFilter(dateFrom, dateTo));
    }
}
