package com.example.mqtt.controller;

import com.example.mqtt.config.NettyMqttServerConfig;
import com.example.mqtt.handler.MqttMessageHandler;
import com.example.mqtt.model.DeviceData;
// import com.example.mqtt.model.MqttMessage; // 在方法中使用全限定名避免冲突
import com.example.mqtt.service.DeviceDataService;
import com.example.mqtt.service.MqttClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT REST API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mqtt")
@CrossOrigin(origins = "*")
public class MqttController {

    private final MqttClientService mqttClientService;
    private final DeviceDataService deviceDataService;
    private final NettyMqttServerConfig nettyMqttServerConfig;

    public MqttController(MqttClientService mqttClientService, 
                         DeviceDataService deviceDataService,
                         NettyMqttServerConfig nettyMqttServerConfig) {
        this.mqttClientService = mqttClientService;
        this.deviceDataService = deviceDataService;
        this.nettyMqttServerConfig = nettyMqttServerConfig;
    }

    /**
     * 获取MQTT服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("server_info", nettyMqttServerConfig.getServerInfo());
        status.put("server_running", nettyMqttServerConfig.isRunning());
        status.put("client_connected", mqttClientService.isConnected());
        status.put("connected_clients", MqttMessageHandler.getConnectedClientCount());
        status.put("device_count", deviceDataService.getDeviceCount());
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        status.put("application_status", "running");
        
        return ResponseEntity.ok(status);
    }

    /**
     * 发布MQTT消息
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishMessage(
            @RequestParam String topic,
            @RequestParam String payload,
            @RequestParam(defaultValue = "1") int qos,
            @RequestParam(defaultValue = "false") boolean retained) {
        
        log.info("收到发布消息请求 - 主题: {}, 内容: {}", topic, payload);
        
        boolean success = mqttClientService.publish(topic, payload, qos, retained);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "消息发布成功" : "消息发布失败");
        response.put("topic", topic);
        response.put("payload", payload);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 订阅MQTT主题
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @RequestParam String topic,
            @RequestParam(defaultValue = "1") int qos) {
        
        log.info("收到订阅主题请求 - 主题: {}, QoS: {}", topic, qos);
        
        boolean success = mqttClientService.subscribe(topic, qos);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "订阅成功" : "订阅失败");
        response.put("topic", topic);
        response.put("qos", qos);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 取消订阅MQTT主题
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestParam String topic) {
        log.info("收到取消订阅请求 - 主题: {}", topic);
        
        boolean success = mqttClientService.unsubscribe(topic);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "取消订阅成功" : "取消订阅失败");
        response.put("topic", topic);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取接收到的MQTT消息
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, com.example.mqtt.model.MqttMessage>> getReceivedMessages() {
        Map<String, com.example.mqtt.model.MqttMessage> messages = mqttClientService.getReceivedMessages();
        return ResponseEntity.ok(messages);
    }

    /**
     * 向特定客户端发送消息
     */
    @PostMapping("/send-to-client")
    public ResponseEntity<Map<String, Object>> sendMessageToClient(
            @RequestParam String clientId,
            @RequestParam String topic,
            @RequestParam String payload) {
        
        log.info("发送消息给客户端 - 客户端ID: {}, 主题: {}, 内容: {}", clientId, topic, payload);
        
        boolean success = MqttMessageHandler.sendMessageToClient(clientId, topic, payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "消息发送成功" : "消息发送失败，客户端可能不在线");
        response.put("client_id", clientId);
        response.put("topic", topic);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有设备数据
     */
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceData>> getAllDevices() {
        List<DeviceData> devices = deviceDataService.getAllDeviceData();
        return ResponseEntity.ok(devices);
    }

    /**
     * 根据设备ID获取设备数据
     */
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceData> getDevice(@PathVariable String deviceId) {
        DeviceData device = deviceDataService.getDeviceData(deviceId);
        if (device != null) {
            return ResponseEntity.ok(device);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 手动发送设备数据
     */
    @PostMapping("/devices/send")
    public ResponseEntity<Map<String, Object>> sendDeviceData(@RequestBody DeviceData deviceData) {
        log.info("收到手动发送设备数据请求: {}", deviceData.getDeviceId());
        
        boolean success = deviceDataService.sendDeviceData(deviceData);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "设备数据发送成功" : "设备数据发送失败");
        response.put("device_id", deviceData.getDeviceId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 发送系统状态
     */
    @PostMapping("/system/status")
    public ResponseEntity<Map<String, Object>> sendSystemStatus() {
        log.info("发送系统状态");
        
        boolean success = deviceDataService.sendSystemStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "系统状态发送成功" : "系统状态发送失败");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发模拟数据
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> triggerSimulation() {
        log.info("手动触发模拟数据发送");
        
        Map<String, Object> response = new HashMap<>();
        
        // 检查MQTT客户端连接状态
        if (!mqttClientService.isConnected()) {
            response.put("success", false);
            response.put("message", "模拟数据发送失败：MQTT客户端未连接");
            response.put("client_connected", false);
            return ResponseEntity.ok(response);
        }
        
        try {
            deviceDataService.sendSimulatedData();
            response.put("success", true);
            response.put("message", "模拟数据发送完成");
            response.put("client_connected", true);
            response.put("device_count", deviceDataService.getDeviceCount());
        } catch (Exception e) {
            log.error("手动触发模拟数据失败", e);
            response.put("success", false);
            response.put("message", "模拟数据发送失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 清理过期数据
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        log.info("清理过期数据");
        
        deviceDataService.cleanupExpiredData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "数据清理完成");
        
        return ResponseEntity.ok(response);
    }
}