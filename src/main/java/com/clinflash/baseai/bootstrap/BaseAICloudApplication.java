package com.clinflash.baseai.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <h1>BaseAICloudApplication</h1>
 *
 * <p>Spring Boot 启动入口。</p>
 */
@SpringBootApplication(scanBasePackages = "com.clinflash.baseai")
public class BaseAICloudApplication {

    /**
     * 项目启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BaseAICloudApplication.class, args);
    }

}
