package com.example.mqtt.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.Map;

/**
 * MQTT消息处理器
 */
@Slf4j
@Component
public class MqttMessageHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<String> CLIENT_ID = AttributeKey.valueOf("clientId");
    private static final ConcurrentHashMap<String, ChannelHandlerContext> clients = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>(); // clientId -> topics
    private static final AtomicInteger messageIdCounter = new AtomicInteger(1);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MqttMessage) {
            MqttMessage mqttMessage = (MqttMessage) msg;
            MqttFixedHeader fixedHeader = mqttMessage.fixedHeader();
            
            log.debug("接收到MQTT消息, 类型: {}, 客户端: {}", 
                fixedHeader.messageType(), getClientId(ctx));

            switch (fixedHeader.messageType()) {
                case CONNECT:
                    handleConnect(ctx, mqttMessage);
                    break;
                case PUBLISH:
                    handlePublish(ctx, mqttMessage);
                    break;
                case SUBSCRIBE:
                    handleSubscribe(ctx, mqttMessage);
                    break;
                case UNSUBSCRIBE:
                    handleUnsubscribe(ctx, mqttMessage);
                    break;
                case PINGREQ:
                    handlePingReq(ctx);
                    break;
                case DISCONNECT:
                    handleDisconnect(ctx);
                    break;
                default:
                    log.warn("未处理的消息类型: {}", fixedHeader.messageType());
                    break;
            }
        }
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttConnectMessage connectMessage = (MqttConnectMessage) msg;
        MqttConnectPayload payload = connectMessage.payload();
        String clientId = payload.clientIdentifier();
        
        log.info("客户端连接: {}", clientId);
        
        // 保存客户端信息
        ctx.channel().attr(CLIENT_ID).set(clientId);
        clients.put(clientId, ctx);
        
        // 发送连接确认
        MqttConnAckMessage connAckMessage = new MqttConnAckMessage(
            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
            new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false)
        );
        
        ctx.writeAndFlush(connAckMessage);
    }

    /**
     * 处理发布消息
     */
    private void handlePublish(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
        String topic = publishMessage.variableHeader().topicName();
        byte[] payload = new byte[publishMessage.payload().readableBytes()];
        publishMessage.payload().readBytes(payload);
        String content = new String(payload);
        
        log.info("收到发布消息 - 主题: {}, 内容: {}, 客户端: {}", 
            topic, content, getClientId(ctx));
        
        // 转发消息给所有订阅了相关主题的客户端（包括发布者）
        forwardMessageToSubscribers(topic, content);
        
        // 这里可以添加业务逻辑处理
        // 比如转发给订阅者、保存到数据库等
        
        // 如果QoS > 0，需要发送PUBACK
        MqttQoS qos = publishMessage.fixedHeader().qosLevel();
        if (qos == MqttQoS.AT_LEAST_ONCE) {
            MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(publishMessage.variableHeader().packetId())
            );
            ctx.writeAndFlush(pubAckMessage);
        }
    }

    /**
     * 处理订阅请求
     */
    private void handleSubscribe(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) msg;
        String clientId = getClientId(ctx);
        
        log.info("客户端订阅请求: {}, 主题: {}", 
            clientId, subscribeMessage.payload().topicSubscriptions());
        
        // 记录订阅关系
        Set<String> clientSubscriptions = subscriptions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet());
        subscribeMessage.payload().topicSubscriptions().forEach(subscription -> {
            clientSubscriptions.add(subscription.topicName());
            log.debug("记录订阅: 客户端={}, 主题={}", clientId, subscription.topicName());
        });
        
        // 发送订阅确认
        MqttSubAckMessage subAckMessage = new MqttSubAckMessage(
            new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
            MqttMessageIdVariableHeader.from(subscribeMessage.variableHeader().messageId()),
            new MqttSubAckPayload(MqttQoS.AT_LEAST_ONCE.value()) // 返回最大支持的QoS
        );
        
        ctx.writeAndFlush(subAckMessage);
    }

    /**
     * 处理取消订阅
     */
    private void handleUnsubscribe(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) msg;
        
        log.info("客户端取消订阅: {}, 主题: {}", 
            getClientId(ctx), unsubscribeMessage.payload().topics());
        
        // 发送取消订阅确认
        MqttUnsubAckMessage unsubAckMessage = new MqttUnsubAckMessage(
            new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
            MqttMessageIdVariableHeader.from(unsubscribeMessage.variableHeader().messageId())
        );
        
        ctx.writeAndFlush(unsubAckMessage);
    }

    /**
     * 处理心跳请求
     */
    private void handlePingReq(ChannelHandlerContext ctx) {
        log.debug("收到心跳请求: {}", getClientId(ctx));
        
        MqttMessage pingResp = new MqttMessage(
            new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0)
        );
        
        ctx.writeAndFlush(pingResp);
    }

    /**
     * 处理断开连接
     */
    private void handleDisconnect(ChannelHandlerContext ctx) {
        String clientId = getClientId(ctx);
        log.info("客户端断开连接: {}", clientId);
        
        if (clientId != null) {
            clients.remove(clientId);
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientId = getClientId(ctx);
        if (clientId != null) {
            clients.remove(clientId);
            subscriptions.remove(clientId); // 清理订阅关系
            log.info("客户端连接断开: {}", clientId);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("MQTT处理异常, 客户端: {}", getClientId(ctx), cause);
        ctx.close();
    }

    /**
     * 获取客户端ID
     */
    private String getClientId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(CLIENT_ID).get();
    }

    /**
     * 获取连接的客户端数量
     */
    public static int getConnectedClientCount() {
        return clients.size();
    }

    /**
     * 向特定客户端发送消息
     */
    public static boolean sendMessageToClient(String clientId, String topic, String payload) {
        ChannelHandlerContext ctx = clients.get(clientId);
        if (ctx != null && ctx.channel().isActive()) {
            MqttPublishMessage publishMessage = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                new MqttPublishVariableHeader(topic, messageIdCounter.getAndIncrement()),
                ctx.alloc().buffer().writeBytes(payload.getBytes())
            );
            ctx.writeAndFlush(publishMessage);
            return true;
        }
        return false;
    }
    
    /**
     * 转发消息给订阅者
     */
    private void forwardMessageToSubscribers(String topic, String payload) {
        log.debug("转发消息到订阅者 - 主题: {}", topic);
        
        int forwardCount = 0;
        for (Map.Entry<String, Set<String>> entry : subscriptions.entrySet()) {
            String clientId = entry.getKey();
            Set<String> topics = entry.getValue();
            
            // 检查该客户端是否订阅了相关主题
            for (String subscribedTopic : topics) {
                if (topicMatches(subscribedTopic, topic)) {
                    boolean sent = sendMessageToClient(clientId, topic, payload);
                    if (sent) {
                        forwardCount++;
                        log.debug("消息转发成功: 客户端={}, 主题={}", clientId, topic);
                    }
                    break; // 每个客户端只转发一次
                }
            }
        }
        
        log.info("消息转发完成 - 主题: {}, 转发数量: {}", topic, forwardCount);
    }
    
    /**
     * 检查主题是否匹配（支持通配符）
     */
    private boolean topicMatches(String subscribedTopic, String publishTopic) {
        if (subscribedTopic.equals(publishTopic)) {
            return true;
        }
        
        // 处理通配符
        if (subscribedTopic.contains("+") || subscribedTopic.contains("#")) {
            String regex = subscribedTopic
                .replace("+", "[^/]+")
                .replace("#", ".*");
            return publishTopic.matches(regex);
        }
        
        return false;
    }
}