package com.example.mqtt.config;

import com.example.mqtt.handler.MqttMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


import java.util.concurrent.TimeUnit;

/**
 * Netty MQTT 服务器配置
 */
@Slf4j
@Component
@Configuration
public class NettyMqttServerConfig {

    @Value("${mqtt.broker.host:0.0.0.0}")
    private String host;

    @Value("${mqtt.broker.port:1883}")
    private int port;

    @Value("${mqtt.broker.boss-thread:1}")
    private int bossThread;

    @Value("${mqtt.broker.worker-thread:4}")
    private int workerThread;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final MqttMessageHandler mqttMessageHandler;

    public NettyMqttServerConfig(MqttMessageHandler mqttMessageHandler) {
        this.mqttMessageHandler = mqttMessageHandler;
    }

    @PostConstruct
    public void start() {
        new Thread(this::startServer, "netty-mqtt-server").start();
    }

    private void startServer() {
        bossGroup = new NioEventLoopGroup(bossThread);
        workerGroup = new NioEventLoopGroup(workerThread);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加空闲状态检测器，90秒没有消息就关闭连接
                            pipeline.addLast("idleStateHandler", 
                                new IdleStateHandler(90, 0, 0, TimeUnit.SECONDS));
                            
                            // MQTT消息解码器
                            pipeline.addLast("decoder", new MqttDecoder());
                            
                            // MQTT消息编码器
                            pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                            
                            // 自定义MQTT消息处理器
                            pipeline.addLast("handler", mqttMessageHandler);
                        }
                    });

            // 绑定端口，同步等待成功
            ChannelFuture future = bootstrap.bind(host, port).sync();
            serverChannel = future.channel();
            
            log.info("Netty MQTT服务器启动成功，监听地址: {}:{}", host, port);
            
            // 等待服务器socket关闭
            serverChannel.closeFuture().sync();
            
        } catch (InterruptedException e) {
            log.error("Netty MQTT服务器启动失败", e);
            Thread.currentThread().interrupt();
        } finally {
            // 优雅关闭
            shutdown();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭Netty MQTT服务器...");
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        log.info("Netty MQTT服务器已关闭");
    }

    /**
     * 获取服务器状态
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }

    /**
     * 获取服务器信息
     */
    public String getServerInfo() {
        return String.format("MQTT服务器 - 地址: %s:%d, 状态: %s, 连接数: %d", 
            host, port, 
            isRunning() ? "运行中" : "已停止",
            MqttMessageHandler.getConnectedClientCount());
    }
}