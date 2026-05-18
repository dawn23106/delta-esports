package com.delta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类。
 * @SpringBootApplication 是三个注解的复合：
 *   @Configuration  - 标记该类为配置类
 *   @EnableAutoConfiguration  - 根据依赖自动配置（如数据源、Redis）
 *   @ComponentScan  - 扫描当前包及其子包中的 @Component/@Service/@Controller
 */
@SpringBootApplication
public class DeltaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeltaApplication.class, args);
    }
}
