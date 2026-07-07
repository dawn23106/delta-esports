package com.delta.esports;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@OpenAPIDefinition(info = @Info(
    title = "Delta Esports API",
    version = "2.0",
    description = "沧月电竞 - 游戏陪玩代练服务平台"))
public class DeltaEsportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeltaEsportsApplication.class, args);
    }
}
