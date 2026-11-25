package com.aomanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "app.auth.max-failed-attempts=3",
        "app.auth.login-rate-limit=3",
        "app.auth.login-rate-window-seconds=60"
})
class SecurityHardeningIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void authenticationSessionsRateLimitLockoutAndHeadersAreEffective() throws Exception {
        String loginBody = objectMapper.writeValueAsString(Map.of("email", "admin@ao.local", "password", "TestAdmin123!"));
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.10.10.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(cookie().httpOnly("ao_refresh", true))
                .andReturn();

        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
        Cookie refreshCookie = login.getResponse().getCookie("ao_refresh");

        mvc.perform(post("/api/auth/refresh")
                        .header("X-Forwarded-For", "10.10.10.1")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(cookie().httpOnly("ao_refresh", true));

        mvc.perform(get("/api/profile/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions[0].active").value(true));

        mvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Max-Age=0"))));

        mvc.perform(get("/api/about"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("Permissions-Policy"));

        String badUnknownBody = objectMapper.writeValueAsString(Map.of("email", "unknown@ao.local", "password", "Wrong123!"));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "10.10.10.2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badUnknownBody))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.10.10.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badUnknownBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

        String badAdminBody = objectMapper.writeValueAsString(Map.of("email", "admin@ao.local", "password", "Wrong123!"));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "10.10.10.3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badAdminBody))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.10.10.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isForbidden());
    }
}
