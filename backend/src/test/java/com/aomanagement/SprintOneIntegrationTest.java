package com.aomanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintOneIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void sprintOneCoreFlow() throws Exception {
        String adminToken = login("admin@ao.local", "TestAdmin123!");

        long employeeOneId = createEmployee(adminToken, "employee1@ao.local", "Employee123!");
        long employeeTwoId = createEmployee(adminToken, "employee2@ao.local", "Employee123!");

        String employeeOneToken = login("employee1@ao.local", "Employee123!");
        login("employee2@ao.local", "Employee123!");

        long tenderOneId = createTender(adminToken, "AO-" + System.nanoTime(), employeeOneId);
        long tenderTwoId = createTender(adminToken, "AO-X-" + System.nanoTime(), employeeTwoId);

        mvc.perform(patch("/api/admin/tenders/{id}/assign/{employeeId}", tenderOneId, employeeOneId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedEmployee.id").value(employeeOneId));

        mvc.perform(get("/api/employee/tenders").header("Authorization", bearer(employeeOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(tenderOneId));

        mvc.perform(get("/api/tenders/{id}", tenderTwoId).header("Authorization", bearer(employeeOneToken)))
                .andExpect(status().isForbidden());

        MockMultipartFile pdf = new MockMultipartFile("file", "rc.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        mvc.perform(multipart("/api/tenders/{id}/documents", tenderOneId)
                        .file(pdf)
                        .param("documentType", "RC")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("RC"));

        mvc.perform(get("/api/tenders/{id}/documents", tenderOneId).header("Authorization", bearer(employeeOneToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/admin/tenders/{id}", tenderTwoId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private long createEmployee(String token, String email, String password) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "firstName", "Test",
                "lastName", email.substring(0, email.indexOf('@')),
                "email", email,
                "password", password,
                "enabled", true
        ));
        String response = mvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createTender(String token, String reference, long employeeId) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "reference", reference,
                "marketNumber", "M-001",
                "title", "Fourniture et installation",
                "organization", "Direction achats",
                "publicationDate", LocalDate.now().toString(),
                "deadline", LocalDate.now().plusDays(10).toString(),
                "priority", "HIGH",
                "assignedEmployeeId", employeeId
        ));
        String response = mvc.perform(post("/api/admin/tenders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
