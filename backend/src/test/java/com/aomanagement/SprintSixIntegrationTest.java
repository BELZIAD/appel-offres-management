package com.aomanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintSixIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void companyBrandingProfileSystemAndBackups() throws Exception {
        String admin = login("admin@ao.local", "TestAdmin123!");

        mvc.perform(get("/api/company-settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").exists());

        mvc.perform(put("/api/admin/company")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("companyName", "Entreprise Test"),
                                Map.entry("applicationName", "Plateforme AO Entreprise"),
                                Map.entry("primaryColor", "#123456"),
                                Map.entry("accentColor", "#0ea5e9"),
                                Map.entry("email", "contact@test.local"),
                                Map.entry("phone", "+212500000000"),
                                Map.entry("ice", "001122334455667"),
                                Map.entry("rc", "12345"),
                                Map.entry("currency", "MAD"),
                                Map.entry("timezone", "Africa/Casablanca"),
                                Map.entry("language", "fr")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Entreprise Test"))
                .andExpect(jsonPath("$.primaryColor").value("#123456"));

        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        mvc.perform(multipart("/api/admin/company/assets/logo").file(logo).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(containsString("/api/assets/branding/")));

        mvc.perform(put("/api/profile")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", "Admin", "lastName", "Dev", "jobTitle", "Directeur", "department", "AO", "phone", "0600000000", "language", "fr", "timezone", "Africa/Casablanca"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Directeur"));

        mvc.perform(put("/api/profile/preferences")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("theme", "LIGHT", "language", "fr", "emailNotifications", true, "browserNotifications", false, "timezone", "Africa/Casablanca", "dateFormat", "dd/MM/yyyy", "timeFormat", "HH:mm", "pageSize", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.pageSize").value(30));

        mvc.perform(get("/api/about").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.applicationName").value("Plateforme AO Entreprise"));

        mvc.perform(get("/api/admin/system").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/admin/backups").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists());

        mvc.perform(get("/api/admin/backups").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backups.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mvc.perform(get("/api/admin/exports/report.pdf").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
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
