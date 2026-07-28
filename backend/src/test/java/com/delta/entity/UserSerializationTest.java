package com.delta.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserSerializationTest {

    @Test
    void passwordHashIsNeverReturnedToFrontend() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800000000");
        user.setPassword("bcrypt-hash");

        String json = new ObjectMapper().writeValueAsString(user);

        assertFalse(json.contains("password"));
        assertFalse(json.contains("bcrypt-hash"));
    }
}
