package com.aomanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintFiveIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void documentAnalysisFlowAndSecurity() throws Exception {
        String admin = login("admin@ao.local", "TestAdmin123!");
        long stamp = System.nanoTime();
        long employeeA = createEmployee(admin, "sprint5a-" + stamp + "@ao.local", "Nadia");
        long employeeB = createEmployee(admin, "sprint5b-" + stamp + "@ao.local", "Omar");
        String tokenA = login("sprint5a-" + stamp + "@ao.local", "Employee123!");
        String tokenB = login("sprint5b-" + stamp + "@ao.local", "Employee123!");
        long tenderA = createTender(admin, "S5A-" + stamp, employeeA, LocalDate.of(2026, 8, 19));
        long tenderB = createTender(admin, "S5B-" + stamp, employeeB, LocalDate.of(2026, 8, 19));

        long documentA = uploadPdf(tokenA, tenderA, "rc-sprint5.pdf", textPdf("""
                Appel d'offres n AO-2026-045
                Objet : Travaux de maintenance technique
                Organisme : Commune Test
                Date limite de depot des offres : 20/08/2026
                Ouverture des plis : 21/08/2026
                Montant estime : 1500000 DH
                Caution provisoire : 50000 DH
                Delai d'execution : 90 jours
                Pieces demandees : Attestation fiscale, Attestation CNSS, Registre de commerce,
                Caution provisoire, Memoire technique, Bordereau des prix.
                Visite des lieux obligatoire le 12/08/2026.
                """));
        long documentB = uploadPdf(admin, tenderB, "other.pdf", textPdf("Date limite de depot des offres : 22/08/2026"));

        String start = mvc.perform(post("/api/tenders/{tenderId}/documents/{documentId}/analyze", tenderA, documentA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();
        long analysisId = objectMapper.readTree(start).get("id").asLong();
        JsonNode completed = waitAnalysis(tokenA, analysisId, "COMPLETED");

        mvc.perform(get("/api/document-analyses/{id}", analysisId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tenders/{tenderId}/documents/{documentId}/analyze", tenderB, documentB).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        assertField(completed, "submissionDeadline", "2026-08-20");
        assertField(completed, "estimatedAmount", "1500000");
        assertField(completed, "provisionalGuarantee", "50000");
        mvc.perform(get("/api/tenders/{id}", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2026-08-19"));

        long deadlineFieldId = fieldId(completed, "submissionDeadline");
        mvc.perform(post("/api/tenders/{id}/analysis/apply-field", tenderA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("field", "submissionDeadline", "value", "2026-08-20", "extractedFieldId", deadlineFieldId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[?(@.field=='submissionDeadline')].validated").exists());
        mvc.perform(get("/api/tenders/{id}", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2026-08-20"));
        mvc.perform(get("/api/tenders/{id}/history", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        mvc.perform(get("/api/tenders/{id}/checklist", tenderA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/tenders/{id}/analysis/apply-checklist", tenderA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("analysisId", analysisId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));
        int checklistSize = objectMapper.readTree(mvc.perform(get("/api/tenders/{id}/checklist", tenderA).header("Authorization", bearer(tokenA))).andReturn().getResponse().getContentAsString()).size();
        mvc.perform(post("/api/tenders/{id}/analysis/apply-checklist", tenderA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("analysisId", analysisId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(checklistSize));

        mvc.perform(get("/api/documents/{id}/search", documentA).param("q", "caution").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snippet").exists());
        mvc.perform(get("/api/documents/{id}/search", documentA).param("q", "caution").header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        long blankDocument = uploadPdf(tokenA, tenderA, "scan.pdf", blankPdf());
        long blankAnalysis = objectMapper.readTree(mvc.perform(post("/api/tenders/{tenderId}/documents/{documentId}/analyze", tenderA, blankDocument).header("Authorization", bearer(tokenA))).andReturn().getResponse().getContentAsString()).get("id").asLong();
        JsonNode review = waitAnalysis(tokenA, blankAnalysis, "REQUIRES_REVIEW");
        org.assertj.core.api.Assertions.assertThat(review.get("requiresOcr").asBoolean()).isTrue();

        long invalidDocument = uploadPdf(tokenA, tenderA, "bad.pdf", "%PDF-not-a-real-pdf".getBytes());
        long invalidAnalysis = objectMapper.readTree(mvc.perform(post("/api/tenders/{tenderId}/documents/{documentId}/analyze", tenderA, invalidDocument).header("Authorization", bearer(tokenA))).andReturn().getResponse().getContentAsString()).get("id").asLong();
        waitAnalysis(tokenA, invalidAnalysis, "FAILED");
    }

    private void assertField(JsonNode analysis, String field, String expected) {
        for (JsonNode node : analysis.get("fields")) {
            if (field.equals(node.get("field").asText())) {
                org.assertj.core.api.Assertions.assertThat(node.get("value").asText()).isEqualTo(expected);
                org.assertj.core.api.Assertions.assertThat(node.get("sourcePage").asInt()).isGreaterThanOrEqualTo(1);
                return;
            }
        }
        throw new AssertionError("Field not found: " + field);
    }

    private long fieldId(JsonNode analysis, String field) {
        for (JsonNode node : analysis.get("fields")) if (field.equals(node.get("field").asText())) return node.get("id").asLong();
        throw new AssertionError("Field not found: " + field);
    }

    private JsonNode waitAnalysis(String token, long id, String expected) throws Exception {
        JsonNode node = null;
        for (int i = 0; i < 30; i++) {
            String response = mvc.perform(get("/api/document-analyses/{id}", id).header("Authorization", bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            node = objectMapper.readTree(response);
            if (expected.equals(node.get("status").asText())) return node;
            Thread.sleep(100);
        }
        throw new AssertionError("Analysis did not reach " + expected + ": " + (node == null ? "null" : node.get("status").asText()));
    }

    private long uploadPdf(String token, long tenderId, String name, byte[] bytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, "application/pdf", bytes);
        String response = mvc.perform(multipart("/api/tenders/{id}/documents", tenderId).file(file).param("documentType", "RC").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private byte[] textPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(50, 760);
                for (String line : text.split("\\R")) {
                    content.showText(line.strip());
                    content.newLineAtOffset(0, -14);
                }
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] blankPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(out);
            return out.toByteArray();
        }
    }

    private long createTender(String token, String reference, long employeeId, LocalDate deadline) throws Exception {
        String response = mvc.perform(post("/api/admin/tenders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reference", reference, "marketNumber", "S5", "title", "Tender Sprint 5", "organization", "Org Sprint 5", "publicationDate", "2026-08-07", "deadline", deadline.toString(), "priority", "NORMAL", "assignedEmployeeId", employeeId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createEmployee(String token, String email, String firstName) throws Exception {
        String response = mvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", firstName, "lastName", "Sprint5", "email", email, "password", "Employee123!", "enabled", true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
