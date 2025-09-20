package com.example.mqtt.service;

// import com.example.mqtt.model.MqttMessage; // 避免与Paho的MqttMessage冲突
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT客户端服务
 */
@Slf4j
@Service
public class MqttClientService {

    @Value("${mqtt.client.server-url:tcp://localhost:1883}")
    private String serverUrl;

    @Value("${mqtt.client.client-id:springboot-mqtt-client}")
    private String clientId;

    @Value("${mqtt.client.username:}")
    private String username;

    @Value("${mqtt.client.password:}")
    private String password;

    @Value("${mqtt.client.timeout:30}")
    private int timeout;

    @Value("${mqtt.client.keep-alive:60}")
    private int keepAlive;

    @Value("${mqtt.client.clean-session:true}")
    private boolean cleanSession;

    @Value("${mqtt.client.subscribe-topics:}")
    private String subscribeTopicsConfig;
    
    private List<Map<String, Object>> subscribeTopics;

    private MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public MqttClientService() {
        // 配置ObjectMapper支持Java 8时间类型
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    // 存储接收到的消息
    private final ConcurrentHashMap<String, com.example.mqtt.model.MqttMessage> receivedMessages = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 解析订阅主题配置
        parseSubscribeTopics();
        
        // 延迟启动MQTT客户端连接，等待服务器完全启动
        scheduler.schedule(this::connectToBroker, 2, TimeUnit.SECONDS);
        
        // 启动定时任务，清理过期消息
        scheduler.scheduleAtFixedRate(this::cleanupExpiredMessages, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 解析订阅主题配置
     */
    private void parseSubscribeTopics() {
        subscribeTopics = new ArrayList<>();
        
        if (subscribeTopicsConfig != null && !subscribeTopicsConfig.trim().isEmpty()) {
            try {
                // 如果是JSON格式，尝试解析
                if (subscribeTopicsConfig.startsWith("[")) {
                    subscribeTopics = objectMapper.readValue(subscribeTopicsConfig, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)));
                } else {
                    log.warn("订阅主题配置格式不正确，使用默认配置");
                    addDefaultSubscribeTopics();
                }
            } catch (Exception e) {
                log.error("解析订阅主题配置失败，使用默认配置", e);
                addDefaultSubscribeTopics();
            }
        } else {
            // 使用默认配置
            addDefaultSubscribeTopics();
        }
    }
    
    /**
     * 添加默认订阅主题
     */
    private void addDefaultSubscribeTopics() {
        subscribeTopics = new ArrayList<>();
        
        Map<String, Object> topic1 = new HashMap<>();
        topic1.put("topic", "device/+/data");
        topic1.put("qos", 1);
        subscribeTopics.add(topic1);
        
        Map<String, Object> topic2 = new HashMap<>();
        topic2.put("topic", "system/status");
        topic2.put("qos", 0);
        subscribeTopics.add(topic2);
    }

    /**
     * 连接到MQTT代理
     */
    public void connectToBroker() {
        try {
            log.info("尝试连接到MQTT代理: {}", serverUrl);
            
            mqttClient = new MqttClient(serverUrl, clientId + "_" + System.currentTimeMillis(), 
                new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(cleanSession);
            options.setConnectionTimeout(timeout);
            options.setKeepAliveInterval(keepAlive);
            options.setAutomaticReconnect(true);

            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
            }
            if (password != null && !password.isEmpty()) {
                options.setPassword(password.toCharArray());
            }

            // 设置回调
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable throwable) {
                    log.warn("MQTT连接丢失，将自动重连", throwable);
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage mqttMessage) throws Exception {
                    handleMessage(topic, mqttMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("消息发送完成: {}", token.getMessageId());
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("MQTT连接成功: {}, 重连: {}", serverURI, reconnect);
                    if (reconnect) {
                        // 重连后重新订阅
                        subscribeDefaultTopics();
                    }
                }
            });

            // 连接
            mqttClient.connect(options);
            log.info("MQTT客户端连接成功: {}", serverUrl);

            // 订阅默认主题
            subscribeDefaultTopics();

        } catch (MqttException e) {
            log.error("MQTT客户端连接失败，将在5秒后重试", e);
            // 在连接失败后延迟重试
            scheduler.schedule(this::connectToBroker, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * 订阅默认主题
     */
    private void subscribeDefaultTopics() {
        if (subscribeTopics != null && !subscribeTopics.isEmpty()) {
            for (Map<String, Object> topicConfig : subscribeTopics) {
                String topic = (String) topicConfig.get("topic");
                int qos = (Integer) topicConfig.getOrDefault("qos", 1);
                subscribe(topic, qos);
            }
        }
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(String topic, org.eclipse.paho.client.mqttv3.MqttMessage pahoMessage) {
        try {
            String payload = new String(pahoMessage.getPayload());
            log.info("收到MQTT消息 - 主题: {}, 内容: {}, QoS: {}", 
                topic, payload, pahoMessage.getQos());

            // 创建自定义消息对象
            com.example.mqtt.model.MqttMessage message = new com.example.mqtt.model.MqttMessage();
            message.setTopic(topic);
            message.setPayload(payload);
            message.setQos(pahoMessage.getQos());
            message.setRetained(pahoMessage.isRetained());
            message.setMessageId(String.valueOf(pahoMessage.getId()));
            message.setClientId(clientId);

            // 存储消息
            receivedMessages.put(message.getMessageId(), message);

            // 这里可以添加业务逻辑处理
            processBusinessLogic(message);

        } catch (Exception e) {
            log.error("处理MQTT消息时出错", e);
        }
    }

    /**
     * 业务逻辑处理
     */
    private void processBusinessLogic(com.example.mqtt.model.MqttMessage message) {
        // 根据主题进行不同的业务处理
        String topic = message.getTopic();
        
        if (topic.startsWith("device/") && topic.endsWith("/data")) {
            // 处理设备数据
            log.info("处理设备数据: {}", message.getPayload());
        } else if (topic.equals("system/status")) {
            // 处理系统状态
            log.info("处理系统状态: {}", message.getPayload());
        }
    }

    /**
     * 发布消息
     */
    public boolean publish(String topic, String payload) {
        return publish(topic, payload, 1, false);
    }

    /**
     * 发布消息
     */
    public boolean publish(String topic, String payload, int qos, boolean retained) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                org.eclipse.paho.client.mqttv3.MqttMessage pahoMessage = new org.eclipse.paho.client.mqttv3.MqttMessage(payload.getBytes());
                pahoMessage.setQos(qos);
                pahoMessage.setRetained(retained);
                
                mqttClient.publish(topic, pahoMessage);
                log.info("发布MQTT消息成功 - 主题: {}, 内容: {}", topic, payload);
                return true;
            } else {
                log.warn("MQTT客户端未连接，无法发布消息");
                return false;
            }
        } catch (MqttException e) {
            log.error("发布MQTT消息失败", e);
            return false;
        }
    }

    /**
     * 订阅主题
     */
    public boolean subscribe(String topic, int qos) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, qos);
                log.info("订阅主题成功: {}, QoS: {}", topic, qos);
                return true;
            } else {
                log.warn("MQTT客户端未连接，无法订阅主题");
                return false;
            }
        } catch (MqttException e) {
            log.error("订阅主题失败: {}", topic, e);
            return false;
        }
    }

    /**
     * 取消订阅主题
     */
    public boolean unsubscribe(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.unsubscribe(topic);
                log.info("取消订阅主题成功: {}", topic);
                return true;
            } else {
                log.warn("MQTT客户端未连接，无法取消订阅");
                return false;
            }
        } catch (MqttException e) {
            log.error("取消订阅主题失败: {}", topic, e);
            return false;
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 获取接收到的消息
     */
    public Map<String, com.example.mqtt.model.MqttMessage> getReceivedMessages() {
        return new ConcurrentHashMap<>(receivedMessages);
    }

    /**
     * 清理过期消息
     */
    private void cleanupExpiredMessages() {
        long currentTime = System.currentTimeMillis();
        receivedMessages.entrySet().removeIf(entry -> {
            com.example.mqtt.model.MqttMessage message = entry.getValue();
            // 清理1小时前的消息
            return currentTime - message.getTimestamp().atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli() > 3600000;
        });
    }

    @PreDestroy
    public void disconnect() {
        scheduler.shutdown();
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT客户端已断开连接");
            } catch (MqttException e) {
                log.error("断开MQTT连接时出错", e);
            }
        }
    }
}