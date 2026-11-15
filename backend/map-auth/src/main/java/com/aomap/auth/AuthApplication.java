package com.aomap.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.aomap.auth", "com.aomap.common"})
@MapperScan("com.aomap.auth.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(AuthApplication.class, args);
    }
}
