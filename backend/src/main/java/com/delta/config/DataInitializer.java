package com.delta.config;

import com.delta.entity.User;
import com.delta.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 系统初始化器 — Spring Boot 启动后自动执行。
 * CommandLineRunner.run() 在所有 Bean 初始化完成后调用。
 *
 * 这里用于自动创建客服账号（13800000000），只创建一次：
 * 先查数据库是否有该手机号，有就跳过，没有才插入。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    public DataInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        User exist = userMapper.findByPhone("13800000000");
        if (exist == null) {
            User cs = new User();
            cs.setPhone("13800000000");
            cs.setPassword(BCrypt.hashpw("cs123456", BCrypt.gensalt(12)));
            cs.setNickname("系统客服");
            cs.setRole("cs");
            userMapper.insert(cs);
            System.out.println("[INIT] 客服账号已创建: 13800000000 / cs123456");
        }
    }
}
