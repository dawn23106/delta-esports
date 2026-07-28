package com.delta.config;

import com.delta.entity.User;
import com.delta.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Creates the configurable demo customer-service account when requested. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    @Value("${app.seed.cs-phone}")
    private String phone;

    @Value("${app.seed.cs-password}")
    private String password;

    public DataInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        if (userMapper.findByPhone(phone) != null) {
            return;
        }
        User cs = new User();
        cs.setPhone(phone);
        cs.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        cs.setNickname("系统客服");
        cs.setRole("cs");
        userMapper.insert(cs);
        System.out.println("[INIT] Demo customer-service account created: " + phone);
    }
}
