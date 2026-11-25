package com.aomanagement;

import com.aomanagement.service.TaskService;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintFourIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TaskService taskService;

    @Test
    void collaborationFlowAndSecurity() throws Exception {
        String admin = login("admin@ao.local", "TestAdmin123!");
        long stamp = System.nanoTime();
        long employeeA = createEmployee(admin, "sprint4a-" + stamp + "@ao.local", "Ahmed");
        long employeeB = createEmployee(admin, "sprint4b-" + stamp + "@ao.local", "Sara");
        String tokenA = login("sprint4a-" + stamp + "@ao.local", "Employee123!");
        String tokenB = login("sprint4b-" + stamp + "@ao.local", "Employee123!");

        long tenderA = createTender(admin, "S4A-" + stamp, employeeA);
        long tenderB = createTender(admin, "S4B-" + stamp, employeeB);

        long taskA = createTask(admin, tenderA, employeeA, "Preparer offre technique", LocalDate.now().plusDays(2));
        long taskB = createTask(admin, tenderB, employeeB, "Preparer offre financiere", LocalDate.now().plusDays(2));

        mvc.perform(get("/api/employee/tasks").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==" + taskA + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id==" + taskB + ")]").doesNotExist());

        mvc.perform(get("/api/employee/tasks/{id}", taskB).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/employee/tasks/{id}/status", taskA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists());

        mvc.perform(patch("/api/employee/tasks/{id}/status", taskA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        mvc.perform(get("/api/notifications").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        long templateId = firstTemplate(admin);
        JsonNode checklist = objectMapper.readTree(mvc.perform(post("/api/admin/tenders/{id}/checklist/from-template/{templateId}", tenderA, templateId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (int i = 0; i < 7; i++) {
            long itemId = checklist.get(i).get("id").asLong();
            mvc.perform(patch("/api/tenders/{tenderId}/checklist/{itemId}", tenderA, itemId)
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":true}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/tenders/{id}", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(70))
                .andExpect(jsonPath("$.checklistComplete").value(false));

        for (int i = 7; i < checklist.size(); i++) {
            long itemId = checklist.get(i).get("id").asLong();
            mvc.perform(patch("/api/tenders/{tenderId}/checklist/{itemId}", tenderA, itemId)
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":true}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/tenders/{id}", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklistComplete").value(true));

        MockMultipartFile file1 = new MockMultipartFile("file", "memoire-v1.pdf", "application/pdf", "%PDF-1.4\nv1".getBytes());
        String uploadResponse = mvc.perform(multipart("/api/tenders/{id}/documents", tenderA)
                        .file(file1)
                        .param("documentType", "TECHNICAL_DOCUMENT")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andReturn().getResponse().getContentAsString();
        long documentId = objectMapper.readTree(uploadResponse).get("id").asLong();

        MockMultipartFile file2 = new MockMultipartFile("file", "memoire-v2.pdf", "application/pdf", "%PDF-1.4\nv2".getBytes());
        mvc.perform(multipart("/api/tenders/{tenderId}/documents/{documentId}/versions", tenderA, documentId)
                        .file(file2)
                        .param("comment", "Version finale")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2));

        JsonNode versions = objectMapper.readTree(mvc.perform(get("/api/tenders/{tenderId}/documents/{documentId}/versions", tenderA, documentId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long oldVersionId = versions.get(1).get("id").asLong();
        mvc.perform(get("/api/document-versions/{versionId}/download", oldVersionId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/document-versions/{versionId}/download", oldVersionId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/tenders/{id}/comments", tenderA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Bonjour @admin@ao.local, dossier pret."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Bonjour @admin@ao.local, dossier pret."));
        mvc.perform(post("/api/tenders/{id}/comments", tenderA)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Interdit"))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/tenders/{id}/favorite", tenderA).header("Authorization", bearer(tokenA))).andExpect(status().isOk());
        mvc.perform(post("/api/tenders/{id}/favorite", tenderA).header("Authorization", bearer(tokenA))).andExpect(status().isOk());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/search").param("q", "S4A-" + stamp).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenders.length()").value(1));
        mvc.perform(get("/api/search").param("q", "S4A-" + stamp).header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenders.length()").value(0));

        long tomorrowTask = createTask(admin, tenderA, employeeA, "Depot demain", LocalDate.now().plusDays(1));
        mvc.perform(patch("/api/admin/tasks/{id}/status", tomorrowTask)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
        taskService.notifyTomorrowDeadlines();
        mvc.perform(get("/api/notifications").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='TASK_DEADLINE_WARNING')]").doesNotExist());

        createTask(admin, tenderA, employeeA, "Signature demain", LocalDate.now().plusDays(1));
        taskService.notifyTomorrowDeadlines();
        mvc.perform(get("/api/notifications").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='TASK_DEADLINE_WARNING')]").exists());
    }

    private long createTask(String token, long tenderId, long employeeId, String title, LocalDate deadline) throws Exception {
        String response = mvc.perform(post("/api/admin/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tenderId", tenderId,
                                "title", title,
                                "assignedEmployeeId", employeeId,
                                "priority", "HIGH",
                                "deadline", deadline.toString(),
                                "status", "TODO"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long firstTemplate(String token) throws Exception {
        String response = mvc.perform(get("/api/admin/checklist-templates").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asLong();
    }

    private long createTender(String token, String reference, long employeeId) throws Exception {
        String response = mvc.perform(post("/api/admin/tenders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reference", reference,
                                "marketNumber", "S4",
                                "title", "Tender Sprint 4",
                                "organization", "Sprint 4 Org",
                                "publicationDate", LocalDate.now().toString(),
                                "deadline", LocalDate.now().plusDays(20).toString(),
                                "priority", "NORMAL",
                                "assignedEmployeeId", employeeId
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createEmployee(String token, String email, String firstName) throws Exception {
        String response = mvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", firstName, "lastName", "Sprint4", "email", email, "password", "Employee123!", "enabled", true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
