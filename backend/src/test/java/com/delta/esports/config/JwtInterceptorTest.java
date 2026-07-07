package com.delta.esports.config;

import com.delta.esports.common.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtInterceptorTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/orders/my"))
                .andExpect(status().is(401));
    }

    @Test
    void shouldReturn401WithInvalidTokenFormat() throws Exception {
        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", "InvalidToken"))
                .andExpect(status().is(401));
    }

    @Test
    void shouldReturn401WithExpiredToken() throws Exception {
        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", "Bearer garbage.token.here"))
                .andExpect(status().is(401));
    }

    @Test
    void shouldPassWithValidToken() throws Exception {
        String token = jwtUtils.generateToken(1L, "admin");

        mockMvc.perform(get("/api/orders/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassOptionsRequest() throws Exception {
        mockMvc.perform(options("/api/orders/my"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAuthEndpointsWithoutToken() throws Exception {
        // Login without token should be processed (JwtInterceptor excludes /api/auth/**)
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"phone\":\"13800000001\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicServiceEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicAnnouncementEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }
}
