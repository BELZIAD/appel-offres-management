package com.aomanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintThreeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void reportingAndExportsFlow() throws Exception {
        String admin = login("admin@ao.local", "TestAdmin123!");
        long stamp = System.nanoTime();
        long employee = createEmployee(admin, "sprint3-" + stamp + "@ao.local");
        String employeeToken = login("sprint3-" + stamp + "@ao.local", "Employee123!");

        LocalDate range50 = LocalDate.of(2031, 1, 1);
        for (int i = 0; i < 10; i++) declareResult(admin, createTender(admin, "S3W-" + stamp + "-" + i, employee, range50), "WON", BigDecimal.valueOf(1000 + i), null);
        for (int i = 0; i < 10; i++) declareResult(admin, createTender(admin, "S3L-" + stamp + "-" + i, employee, range50), "LOST", null, "Prix eleve");

        mvc.perform(get("/api/admin/statistics/overview")
                        .param("dateFrom", "2031-01-01")
                        .param("dateTo", "2031-12-31")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(10))
                .andExpect(jsonPath("$.lost").value(10))
                .andExpect(jsonPath("$.winRate").value(50.0))
                .andExpect(jsonPath("$.totalWonAmount").value(10045));

        LocalDate range100 = LocalDate.of(2032, 1, 1);
        for (int i = 0; i < 5; i++) declareResult(admin, createTender(admin, "S3W100-" + stamp + "-" + i, employee, range100), "WON", BigDecimal.valueOf(2000), null);
        mvc.perform(get("/api/admin/statistics/overview")
                        .param("dateFrom", "2032-01-01")
                        .param("dateTo", "2032-12-31")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winRate").value(100.0));

        mvc.perform(get("/api/admin/statistics/overview")
                        .param("dateFrom", "2040-01-01")
                        .param("dateTo", "2040-12-31")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winRate").value(0));

        long pendingTender = createTender(admin, "S3P-" + stamp, employee, LocalDate.of(2033, 1, 1));
        declareResult(admin, pendingTender, "WON", BigDecimal.valueOf(3000), null)
                .andExpect(jsonPath("$.resultDate").exists())
                .andExpect(jsonPath("$.finalAmount").value(3000));
        declareResult(admin, pendingTender, "PENDING", null, null)
                .andExpect(jsonPath("$.resultDate").doesNotExist());

        mvc.perform(get("/api/admin/statistics/employees").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.employeeId==" + employee + ")].assigned").exists());

        mvc.perform(get("/api/admin/statistics/overview")
                        .param("dateFrom", "2031-01-01")
                        .param("dateTo", "2031-01-01")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTenders").value(20));

        mvc.perform(get("/api/admin/exports/tenders.xlsx").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("spreadsheetml")));

        mvc.perform(get("/api/admin/exports/tenders.xlsx").header("Authorization", bearer(employeeToken)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/exports/report.pdf").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mvc.perform(get("/api/admin/history").param("page", "0").param("size", "5").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    private org.springframework.test.web.servlet.ResultActions declareResult(String token, long tenderId, String result, BigDecimal finalAmount, String lossReason) throws Exception {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("result", result);
        if (finalAmount != null) body.put("finalAmount", finalAmount);
        if (lossReason != null) body.put("lossReason", lossReason);
        return mvc.perform(patch("/api/admin/tenders/{id}/result", tenderId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private long createTender(String token, String reference, long employeeId, LocalDate publicationDate) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "reference", reference,
                "marketNumber", "S3",
                "title", "Reporting tender",
                "organization", "Sprint 3 Org",
                "publicationDate", publicationDate.toString(),
                "deadline", publicationDate.plusDays(20).toString(),
                "estimatedAmount", 5000,
                "priority", "NORMAL",
                "assignedEmployeeId", employeeId
        ));
        String response = mvc.perform(post("/api/admin/tenders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private long createEmployee(String token, String email) throws Exception {
        String response = mvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", "Sprint3", "lastName", "Employee", "email", email, "password", "Employee123!", "enabled", true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
