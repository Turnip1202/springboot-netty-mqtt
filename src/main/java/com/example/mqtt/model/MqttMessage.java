package com.example.mqtt.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * MQTT消息模型
 */
@Data
public class MqttMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 主题
     */
    private String topic;
    
    /**
     * 消息内容
     */
    private String payload;
    
    /**
     * QoS等级
     */
    private int qos;
    
    /**
     * 是否保留消息
     */
    private boolean retained;
    
    /**
     * 发送时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    public MqttMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public MqttMessage(String topic, String payload) {
        this();
        this.topic = topic;
        this.payload = payload;
        this.qos = 0;
        this.retained = false;
    }
    
    public MqttMessage(String topic, String payload, int qos) {
        this(topic, payload);
        this.qos = qos;
    }

    public MqttMessage(byte[] bytes) {
        this.payload = new String(bytes);


    }
}