package com.example.mqtt.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * MQTT客户端服务测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "mqtt.client.server-url=tcp://localhost:1883",
    "mqtt.broker.port=1884" // 使用不同端口避免冲突
})
class MqttClientServiceTest {

    @Test
    void contextLoads() {
        // 测试Spring上下文能够正常加载
    }
}