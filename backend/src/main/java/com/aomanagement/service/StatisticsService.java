package com.aomanagement.service;

import com.aomanagement.dto.StatisticsDtos;
import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final EntityManager entityManager;
    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final MapperService mapperService;

    @Transactional(readOnly = true)
    public StatisticsDtos.OverviewResponse overview(StatisticsDtos.StatisticsFilter filter) {
        String where = dateWhere(filter);
        long total = count("select count(t) from Tender t " + where, filter);
        long inProgress = countStatus(TenderStatus.IN_PROGRESS, filter);
        long submitted = countStatus(TenderStatus.SUBMITTED, filter);
        long won = countResult(TenderResult.WON, filter);
        long lost = countResult(TenderResult.LOST, filter);
        long pending = countResult(TenderResult.PENDING, filter);
        long cancelled = countResult(TenderResult.CANCELLED, filter);
        BigDecimal estimated = sum("select coalesce(sum(t.estimatedAmount), 0) from Tender t " + where, filter);
        BigDecimal wonAmount = sum("select coalesce(sum(t.finalAmount), 0) from Tender t " + where + resultAnd(where, TenderResult.WON), filter);
        BigDecimal avgWon = won == 0 ? BigDecimal.ZERO : wonAmount.divide(BigDecimal.valueOf(won), 2, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now();
        long urgent = entityManager.createQuery("select count(t) from Tender t where t.status <> :closed and t.deadline between :from and :to", Long.class)
                .setParameter("closed", TenderStatus.CLOSED).setParameter("from", today.plusDays(1)).setParameter("to", today.plusDays(2)).getSingleResult();
        long overdue = entityManager.createQuery("select count(t) from Tender t where t.status <> :closed and t.deadline < :today", Long.class)
                .setParameter("closed", TenderStatus.CLOSED).setParameter("today", today).getSingleResult();
        return new StatisticsDtos.OverviewResponse(total, inProgress, submitted, won, lost, pending, cancelled, rate(won, lost), estimated, wonAmount, avgWon, urgent, overdue);
    }

    @Transactional(readOnly = true)
    public List<StatisticsDtos.MonthlyResponse> monthly(int year, StatisticsDtos.StatisticsFilter filter) {
        List<StatisticsDtos.MonthlyResponse> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate from = LocalDate.of(year, month, 1);
            LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
            StatisticsDtos.StatisticsFilter f = intersect(filter, from, to);
            result.add(new StatisticsDtos.MonthlyResponse(month, countAll(f), countResult(TenderResult.WON, f), countResult(TenderResult.LOST, f), countResult(TenderResult.PENDING, f)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StatisticsDtos.YearlyResponse> yearly(StatisticsDtos.StatisticsFilter filter) {
        List<Object[]> rows = entityManager.createQuery("""
                select year(coalesce(t.publicationDate, cast(t.createdAt as localdate))), count(t),
                sum(case when t.result = 'WON' then 1 else 0 end),
                sum(case when t.result = 'LOST' then 1 else 0 end),
                coalesce(sum(case when t.result = 'WON' then t.finalAmount else 0 end), 0)
                from Tender t
                group by year(coalesce(t.publicationDate, cast(t.createdAt as localdate)))
                order by year(coalesce(t.publicationDate, cast(t.createdAt as localdate)))
                """, Object[].class).getResultList();
        return rows.stream().map(r -> {
            long won = number(r[2]);
            long lost = number(r[3]);
            return new StatisticsDtos.YearlyResponse(number(r[0]).intValue(), number(r[1]), won, lost, rate(won, lost), money(r[4]));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<StatisticsDtos.EmployeePerformanceResponse> employees(String sort) {
        List<User> employees = userRepository.findByRoleOrderByLastNameAscFirstNameAsc(Role.EMPLOYEE);
        Comparator<StatisticsDtos.EmployeePerformanceResponse> comparator = Comparator.comparing(StatisticsDtos.EmployeePerformanceResponse::employeeName);
        if ("won".equalsIgnoreCase(sort)) comparator = Comparator.comparing(StatisticsDtos.EmployeePerformanceResponse::won).reversed();
        if ("winRate".equalsIgnoreCase(sort)) comparator = Comparator.comparing(StatisticsDtos.EmployeePerformanceResponse::winRate).reversed();
        if ("assigned".equalsIgnoreCase(sort)) comparator = Comparator.comparing(StatisticsDtos.EmployeePerformanceResponse::assigned).reversed();
        if ("wonAmount".equalsIgnoreCase(sort)) comparator = Comparator.comparing(StatisticsDtos.EmployeePerformanceResponse::wonAmount).reversed();
        return employees.stream().map(this::employeePerformance).sorted(comparator).toList();
    }

    @Transactional(readOnly = true)
    public StatisticsDtos.EmployeeDetailResponse employeeDetail(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Employe introuvable"));
        var tenders = tenderRepository.findByAssignedEmployeeIdOrderByDeadlineAsc(id).stream().map(t -> mapperService.toTenderResponse(t, false)).toList();
        long closed = tenders.stream().filter(t -> t.status() == TenderStatus.CLOSED || t.result() == TenderResult.WON || t.result() == TenderResult.LOST).count();
        return new StatisticsDtos.EmployeeDetailResponse(employeePerformance(user), closed, tenders, monthly(LocalDate.now().getYear(), null));
    }

    @Transactional(readOnly = true)
    public List<StatisticsDtos.OrganizationResponse> organizations(StatisticsDtos.StatisticsFilter filter) {
        String where = dateWhere(filter);
        List<Object[]> rows = setDates(entityManager.createQuery("""
                select t.organization, count(t),
                sum(case when t.result = 'WON' then 1 else 0 end),
                sum(case when t.result = 'LOST' then 1 else 0 end),
                sum(case when t.result = 'PENDING' then 1 else 0 end),
                coalesce(sum(t.estimatedAmount), 0),
                coalesce(sum(case when t.result = 'WON' then t.finalAmount else 0 end), 0)
                from Tender t %s
                group by t.organization
                order by count(t) desc
                """.formatted(where), Object[].class), filter).getResultList();
        return rows.stream().map(r -> {
            long won = number(r[2]);
            long lost = number(r[3]);
            return new StatisticsDtos.OrganizationResponse((String) r[0], number(r[1]), won, lost, number(r[4]), rate(won, lost), money(r[5]), money(r[6]));
        }).toList();
    }

    @Transactional(readOnly = true)
    public StatisticsDtos.ResultsResponse results(StatisticsDtos.StatisticsFilter filter) {
        return new StatisticsDtos.ResultsResponse(countResult(TenderResult.WON, filter), countResult(TenderResult.LOST, filter), countResult(TenderResult.PENDING, filter), countResult(TenderResult.CANCELLED, filter));
    }

    private StatisticsDtos.EmployeePerformanceResponse employeePerformance(User user) {
        Long id = user.getId();
        long assigned = tenderRepository.countByAssignedEmployeeId(id);
        long inProgress = tenderRepository.countByAssignedEmployeeIdAndStatus(id, TenderStatus.IN_PROGRESS);
        long submitted = tenderRepository.countByAssignedEmployeeIdAndStatus(id, TenderStatus.SUBMITTED);
        long won = tenderRepository.countByAssignedEmployeeIdAndResult(id, TenderResult.WON);
        long lost = tenderRepository.countByAssignedEmployeeIdAndResult(id, TenderResult.LOST);
        long pending = tenderRepository.countByAssignedEmployeeIdAndResult(id, TenderResult.PENDING);
        BigDecimal wonAmount = entityManager.createQuery("select coalesce(sum(t.finalAmount), 0) from Tender t where t.assignedEmployee.id = :id and t.result = :won", BigDecimal.class)
                .setParameter("id", id).setParameter("won", TenderResult.WON).getSingleResult();
        return new StatisticsDtos.EmployeePerformanceResponse(id, user.getFirstName() + " " + user.getLastName(), user.getEmail(), user.isEnabled(), assigned, inProgress, submitted, won, lost, pending, rate(won, lost), wonAmount);
    }

    private long countAll(StatisticsDtos.StatisticsFilter filter) { return count("select count(t) from Tender t " + dateWhere(filter), filter); }
    private long countStatus(TenderStatus status, StatisticsDtos.StatisticsFilter filter) { return setDates(entityManager.createQuery("select count(t) from Tender t " + dateWhere(filter) + statusAnd(dateWhere(filter)), Long.class).setParameter("status", status), filter).getSingleResult(); }
    private long countResult(TenderResult result, StatisticsDtos.StatisticsFilter filter) { return setDates(entityManager.createQuery("select count(t) from Tender t " + dateWhere(filter) + resultAnd(dateWhere(filter), result), Long.class), filter).getSingleResult(); }
    private long count(String jpql, StatisticsDtos.StatisticsFilter filter) { return setDates(entityManager.createQuery(jpql, Long.class), filter).getSingleResult(); }
    private BigDecimal sum(String jpql, StatisticsDtos.StatisticsFilter filter) { return money(setDates(entityManager.createQuery(jpql, BigDecimal.class), filter).getSingleResult()); }

    private String dateWhere(StatisticsDtos.StatisticsFilter filter) {
        if (filter == null || (filter.dateFrom() == null && filter.dateTo() == null)) return "";
        List<String> parts = new ArrayList<>();
        if (filter.dateFrom() != null) parts.add("coalesce(t.publicationDate, cast(t.createdAt as localdate)) >= :dateFrom");
        if (filter.dateTo() != null) parts.add("coalesce(t.publicationDate, cast(t.createdAt as localdate)) <= :dateTo");
        return " where " + String.join(" and ", parts);
    }

    private String statusAnd(String where) { return where.isBlank() ? " where t.status = :status" : " and t.status = :status"; }
    private String resultAnd(String where, TenderResult result) { return (where.isBlank() ? " where " : " and ") + "t.result = '" + result.name() + "'"; }

    private <T extends jakarta.persistence.Query> T setDates(T query, StatisticsDtos.StatisticsFilter filter) {
        if (filter != null && filter.dateFrom() != null) query.setParameter("dateFrom", filter.dateFrom());
        if (filter != null && filter.dateTo() != null) query.setParameter("dateTo", filter.dateTo());
        return query;
    }

    private StatisticsDtos.StatisticsFilter intersect(StatisticsDtos.StatisticsFilter filter, LocalDate from, LocalDate to) {
        LocalDate start = filter != null && filter.dateFrom() != null && filter.dateFrom().isAfter(from) ? filter.dateFrom() : from;
        LocalDate end = filter != null && filter.dateTo() != null && filter.dateTo().isBefore(to) ? filter.dateTo() : to;
        return new StatisticsDtos.StatisticsFilter(start, end);
    }

    private BigDecimal rate(long won, long lost) {
        long denominator = won + lost;
        if (denominator == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(won).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private Long number(Object value) { return ((Number) value).longValue(); }
    private BigDecimal money(Object value) { return value == null ? BigDecimal.ZERO : (value instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) value).doubleValue())); }
}
