package com.example.mqtt.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 设备数据模型
 */
@Data
public class DeviceData {
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 设备名称
     */
    private String deviceName;
    
    /**
     * 设备类型
     */
    private String deviceType;
    
    /**
     * 温度
     */
    private Double temperature;
    
    /**
     * 湿度
     */
    private Double humidity;
    
    /**
     * 电池电量
     */
    private Integer battery;
    
    /**
     * 设备状态 (online/offline)
     */
    private String status;
    
    /**
     * 数据时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 位置信息
     */
    private Location location;
    
    /**
     * 位置信息内部类
     */
    @Data
    public static class Location {
        private Double latitude;  // 纬度
        private Double longitude; // 经度
        private String address;   // 地址
    }
    
    public DeviceData() {
        this.timestamp = LocalDateTime.now();
        this.status = "online";
    }
}