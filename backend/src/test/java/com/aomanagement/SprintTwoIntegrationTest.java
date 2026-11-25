package com.aomanagement;

import com.aomanagement.service.EmailService;
import com.aomanagement.service.TenderReminderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintTwoIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenderReminderService reminderService;

    @MockBean
    EmailService emailService;

    @Test
    void sprintTwoCoreFlow() throws Exception {
        String adminToken = login("admin@ao.local", "TestAdmin123!");
        long employeeA = createEmployee(adminToken, "sprint2a@ao.local");
        long employeeB = createEmployee(adminToken, "sprint2b@ao.local");
        String tokenA = login("sprint2a@ao.local", "Employee123!");
        String tokenB = login("sprint2b@ao.local", "Employee123!");

        long tenderA = createTender(adminToken, "S2-A-" + System.nanoTime(), employeeA, LocalDate.now().plusDays(2), "IN_PROGRESS");
        long tenderB = createTender(adminToken, "S2-B-" + System.nanoTime(), employeeB, LocalDate.now().minusDays(1), "NEW");

        mvc.perform(get("/api/notifications").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        long notificationId = objectMapper.readTree(mvc.perform(get("/api/notifications").header("Authorization", bearer(tokenA)))
                .andReturn().getResponse().getContentAsString()).get("content").get(0).get("id").asLong();

        mvc.perform(patch("/api/notifications/{id}/read", notificationId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/employee/tenders/{id}/status", tenderA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));

        mvc.perform(patch("/api/employee/tenders/{id}/status", tenderB)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/admin/tenders/{id}/result", tenderA)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"WON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("WON"));

        mvc.perform(patch("/api/admin/tenders/{id}/assign/{employeeId}", tenderA, employeeB).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedEmployee.id").value(employeeB));

        mvc.perform(get("/api/tenders/{id}/history", tenderA).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(4))));

        MockMultipartFile pdf = new MockMultipartFile("file", "rc.pdf", "application/pdf", "%PDF-1.4 file".getBytes());
        String docResponse = mvc.perform(multipart("/api/tenders/{id}/documents", tenderB)
                        .file(pdf)
                        .param("documentType", "TECHNICAL_DOCUMENT")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileSize").value(greaterThanOrEqualTo(1)))
                .andReturn().getResponse().getContentAsString();
        long docId = objectMapper.readTree(docResponse).get("id").asLong();

        mvc.perform(get("/api/documents/{id}/download", docId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/tenders")
                        .param("search", "S2-A")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        mvc.perform(get("/api/admin/tenders")
                        .param("status", "NEW")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content[0].status").value("NEW"));

        mvc.perform(get("/api/tenders/{id}", tenderB).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineLevel").value("OVERDUE"));

        mvc.perform(get("/api/tenders/{id}", tenderA).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineLevel").value("URGENT"));

        int processed = reminderService.processDeadlineTwoDays(LocalDate.now());
        int processedAgain = reminderService.processDeadlineTwoDays(LocalDate.now());
        org.assertj.core.api.Assertions.assertThat(processed).isGreaterThanOrEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(processedAgain).isZero();
        verify(emailService, times(1)).send(org.mockito.ArgumentMatchers.eq("sprint2b@ao.local"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        mvc.perform(get("/api/admin/email-reminders").param("status", "SENT").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
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
        String json = objectMapper.writeValueAsString(Map.of(
                "firstName", "Sprint",
                "lastName", email.substring(0, email.indexOf('@')),
                "email", email,
                "password", "Employee123!",
                "enabled", true
        ));
        String response = mvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createTender(String token, String reference, long employeeId, LocalDate deadline, String status) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "reference", reference,
                "marketNumber", "M-S2",
                "title", "Sprint 2 tender",
                "organization", "Organisation Sprint 2",
                "publicationDate", LocalDate.now().minusDays(1).toString(),
                "deadline", deadline.toString(),
                "priority", "URGENT",
                "status", status,
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
