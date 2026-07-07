package com.delta.esports.service;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.dto.LoginRequest;
import com.delta.esports.dto.LoginResponse;
import com.delta.esports.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    private static final String TEST_PHONE = "13899999999";
    private static final String TEST_PASSWORD = "test123";

    @org.junit.jupiter.api.Order(1)
    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);
        req.setNickname("测试用户");
        req.setRole("boss");

        LoginResponse resp = userService.register(req);

        assertNotNull(resp.getAccessToken());
        assertEquals(TEST_PHONE, resp.getPhone());
        assertEquals("boss", resp.getRole());
    }

    @org.junit.jupiter.api.Order(2)
    @Test
    void shouldFailRegisterOnDuplicatePhone() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);
        req.setNickname("重复用户");

        assertThrows(BusinessException.class, () -> userService.register(req));
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest req = new LoginRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);

        LoginResponse resp = userService.login(req);

        assertNotNull(resp.getAccessToken());
        assertEquals(TEST_PHONE, resp.getPhone());
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword("wrongpassword");

        assertThrows(BusinessException.class, () -> userService.login(req));
    }

    @Test
    void shouldRefreshToken() {
        LoginRequest req = new LoginRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);
        LoginResponse loginResp = userService.login(req);

        LoginResponse refreshResp = userService.refreshToken(loginResp.getRefreshToken());

        assertNotNull(refreshResp.getAccessToken());
        assertNotNull(refreshResp.getRefreshToken());
    }
}
