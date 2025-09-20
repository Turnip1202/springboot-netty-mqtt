package com.example.mqtt.service;

import com.example.mqtt.model.DeviceData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 设备数据服务
 */
@Slf4j
@Service
public class DeviceDataService {

    private final MqttClientService mqttClientService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 存储设备数据
    private final ConcurrentHashMap<String, DeviceData> deviceDataMap = new ConcurrentHashMap<>();
    
    public DeviceDataService(MqttClientService mqttClientService) {
        this.mqttClientService = mqttClientService;
        // 配置ObjectMapper支持Java 8时间类型
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启动模拟数据发送任务
        startSimulationTask();
    }

    /**
     * 启动模拟数据发送任务
     */
    private void startSimulationTask() {
        // 等待MQTT客户端连接后再开始发送模拟数据
        scheduler.scheduleAtFixedRate(this::sendSimulatedData, 15, 30, TimeUnit.SECONDS);
    }

    /**
     * 发送模拟设备数据
     */
    public void sendSimulatedData() {
        try {
            // 检查MQTT客户端连接状态
            if (!mqttClientService.isConnected()) {
                log.warn("模拟数据发送跳过，MQTT客户端未连接");
                return;
            }
            
            log.info("开始发送模拟设备数据...");
            
            // 模拟设备1
            DeviceData device1 = createSimulatedDevice("DEV001", "温湿度传感器", "sensor");
            device1.setTemperature(20.0 + Math.random() * 15); // 20-35度
            device1.setHumidity(40.0 + Math.random() * 40);    // 40-80%
            device1.setBattery((int)(80 + Math.random() * 20)); // 80-100%
            
            boolean success1 = sendDeviceData(device1);
            log.info("设备1数据发送结果: {}, 设备ID: {}", success1 ? "成功" : "失败", device1.getDeviceId());

            // 模拟设备2
            DeviceData device2 = createSimulatedDevice("DEV002", "环境监测器", "monitor");
            device2.setTemperature(15.0 + Math.random() * 20); // 15-35度
            device2.setHumidity(30.0 + Math.random() * 50);    // 30-80%
            device2.setBattery((int)(60 + Math.random() * 40)); // 60-100%
            
            boolean success2 = sendDeviceData(device2);
            log.info("设备2数据发送结果: {}, 设备ID: {}", success2 ? "成功" : "失败", device2.getDeviceId());

            log.info("发送模拟设备数据完成 - 成功: {}, 失败: {}", 
                (success1 ? 1 : 0) + (success2 ? 1 : 0), 
                (success1 ? 0 : 1) + (success2 ? 0 : 1));
            
        } catch (Exception e) {
            log.error("发送模拟设备数据失败", e);
        }
    }

    /**
     * 创建模拟设备
     */
    private DeviceData createSimulatedDevice(String deviceId, String deviceName, String deviceType) {
        DeviceData device = new DeviceData();
        device.setDeviceId(deviceId);
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setTimestamp(LocalDateTime.now());
        
        // 设置位置信息
        DeviceData.Location location = new DeviceData.Location();
        location.setLatitude(39.9042 + (Math.random() - 0.5) * 0.01); // 北京附近
        location.setLongitude(116.4074 + (Math.random() - 0.5) * 0.01);
        location.setAddress("北京市朝阳区");
        device.setLocation(location);
        
        return device;
    }

    /**
     * 发送设备数据到MQTT
     */
    public boolean sendDeviceData(DeviceData deviceData) {
        try {
            String topic = "device/" + deviceData.getDeviceId() + "/data";
            String payload = objectMapper.writeValueAsString(deviceData);
            
            log.debug("准备发送设备数据 - 主题: {}, 设备: {}", topic, deviceData.getDeviceId());
            
            boolean success = mqttClientService.publish(topic, payload, 1, false);
            
            if (success) {
                // 保存到本地存储
                deviceDataMap.put(deviceData.getDeviceId(), deviceData);
                log.info("设备数据发送成功: {} - 温度: {}°C, 湿度: {}%, 电量: {}%", 
                    deviceData.getDeviceId(), 
                    String.format("%.1f", deviceData.getTemperature()),
                    String.format("%.1f", deviceData.getHumidity()),
                    deviceData.getBattery());
            } else {
                log.warn("设备数据发送失败: {}", deviceData.getDeviceId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("发送设备数据失败: {}", deviceData.getDeviceId(), e);
            return false;
        }
    }

    /**
     * 发送系统状态
     */
    public boolean sendSystemStatus() {
        try {
            Map<String, Object> status = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "server_status", "running",
                "connected_devices", deviceDataMap.size(),
                "memory_usage", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                "uptime", System.currentTimeMillis()
            );
            
            String payload = objectMapper.writeValueAsString(status);
            return mqttClientService.publish("system/status", payload, 0, false);
            
        } catch (Exception e) {
            log.error("发送系统状态失败", e);
            return false;
        }
    }

    /**
     * 获取所有设备数据
     */
    public List<DeviceData> getAllDeviceData() {
        return new ArrayList<>(deviceDataMap.values());
    }

    /**
     * 根据设备ID获取数据
     */
    public DeviceData getDeviceData(String deviceId) {
        return deviceDataMap.get(deviceId);
    }

    /**
     * 更新设备数据
     */
    public void updateDeviceData(DeviceData deviceData) {
        deviceDataMap.put(deviceData.getDeviceId(), deviceData);
    }

    /**
     * 删除设备数据
     */
    public boolean removeDeviceData(String deviceId) {
        return deviceDataMap.remove(deviceId) != null;
    }

    /**
     * 获取设备数量
     */
    public int getDeviceCount() {
        return deviceDataMap.size();
    }

    /**
     * 清理过期设备数据
     */
    public void cleanupExpiredData() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        deviceDataMap.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(expireTime));
    }
}