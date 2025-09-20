package com.example.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * SpringBoot Netty MQTT 示例应用启动类
 */
@Slf4j
@SpringBootApplication
public class MqttApplication {

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(MqttApplication.class, args);
            
            log.info("==============================================");
            log.info("SpringBoot Netty MQTT 应用启动成功!");
            log.info("访问地址: http://localhost:8080");
            log.info("API文档: http://localhost:8080/api/mqtt/status");
            log.info("MQTT服务器: tcp://localhost:1883");
            log.info("==============================================");
            
        } catch (Exception e) {
            log.error("应用启动失败", e);
            System.exit(1);
        }
    }
}