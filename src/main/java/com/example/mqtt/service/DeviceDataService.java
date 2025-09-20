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
        scheduler.scheduleAtFixedRate(this::sendSimulatedData, 10, 30, TimeUnit.SECONDS);
    }

    /**
     * 发送模拟设备数据
     */
    public void sendSimulatedData() {
        try {
            // 模拟设备1
            DeviceData device1 = createSimulatedDevice("DEV001", "温湿度传感器", "sensor");
            device1.setTemperature(20.0 + Math.random() * 15); // 20-35度
            device1.setHumidity(40.0 + Math.random() * 40);    // 40-80%
            device1.setBattery((int)(80 + Math.random() * 20)); // 80-100%
            
            sendDeviceData(device1);

            // 模拟设备2
            DeviceData device2 = createSimulatedDevice("DEV002", "环境监测器", "monitor");
            device2.setTemperature(15.0 + Math.random() * 20); // 15-35度
            device2.setHumidity(30.0 + Math.random() * 50);    // 30-80%
            device2.setBattery((int)(60 + Math.random() * 40)); // 60-100%
            
            sendDeviceData(device2);

            log.info("发送模拟设备数据完成");
            
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
            
            boolean success = mqttClientService.publish(topic, payload, 1, false);
            
            if (success) {
                // 保存到本地存储
                deviceDataMap.put(deviceData.getDeviceId(), deviceData);
                log.debug("设备数据发送成功: {}", deviceData.getDeviceId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("发送设备数据失败", e);
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