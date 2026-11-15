package com.aomap.workspace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.aomap.workspace", "com.aomap.common"})
@MapperScan("com.aomap.workspace.mapper")
public class WorkspaceApplication {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(WorkspaceApplication.class, args);
    }
}
