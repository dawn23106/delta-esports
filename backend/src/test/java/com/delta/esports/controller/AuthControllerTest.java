package com.delta.esports.controller;

import com.delta.esports.common.JwtUtils;
import com.delta.esports.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtils jwtUtils;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800000001");
        req.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone("13800000001");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldFailLoginWithInvalidPhoneFormat() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPhone("12345");
        req.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRejectMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is(401));
    }

    @Test
    void shouldGetMeWithValidToken() throws Exception {
        String token = jwtUtils.generateToken(1L, "admin");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void shouldReturn401WithExpiredToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer expired.invalid.token"))
                .andExpect(status().is(401));
    }

    @Test
    void shouldRejectRefreshTokenAsApiAccessToken() throws Exception {
        String refreshToken = jwtUtils.generateRefreshToken(2L);
        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().is(401));
    }

    @Test
    void shouldAccessPublicEndpointsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldRejectAdminAccessForNonAdmin() throws Exception {
        String token = jwtUtils.generateToken(2L, "boss");

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldRejectServiceMutationForNonAdmin() throws Exception {
        String token = jwtUtils.generateToken(2L, "boss");
        String body = "{\"name\":\"非法服务\",\"category\":\"陪玩专区\",\"basePrice\":1}";

        mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldNotExposeBoosterPhoneOrCredentials() throws Exception {
        String token = jwtUtils.generateToken(2L, "boss");

        mockMvc.perform(get("/api/users/boosters")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].openId").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].phone").doesNotExist());
    }
}
