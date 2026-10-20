package com.aomap.ingest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.aomap.ingest", "com.aomap.common"})
@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("com.aomap.ingest.mapper")
public class IngestApplication {

    public static void main(String[] args) {
        // 固定走 IPv4，避免错误的 IPv6 地址把连接超时占满。
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(IngestApplication.class, args);
    }
}
