# SpringBoot Netty MQTT 示例项目

这是一个基于SpringBoot、Netty和MQTT的完整示例项目，演示了如何构建一个高性能的MQTT服务器和客户端。

## 🚀 项目特性

- **SpringBoot 3.2.0** - 现代化的Spring框架
- **Netty 4.1.x** - 高性能异步网络框架  
- **MQTT 3.1.1** - 轻量级消息传输协议
- **自动重连** - 客户端断线自动重连机制
- **REST API** - 完整的HTTP接口用于管理MQTT
- **Web界面** - 简洁的测试和监控页面
- **设备模拟** - 自动生成模拟设备数据
- **实时监控** - 服务器状态和连接监控

## 📁 项目结构

```
springboot-netty-mqtt/
├── src/main/java/com/example/mqtt/
│   ├── config/
│   │   └── NettyMqttServerConfig.java     # Netty MQTT服务器配置
│   ├── controller/
│   │   └── MqttController.java            # REST API控制器
│   ├── handler/
│   │   └── MqttMessageHandler.java        # MQTT消息处理器
│   ├── model/
│   │   ├── DeviceData.java                # 设备数据模型
│   │   └── MqttMessage.java               # MQTT消息模型
│   ├── service/
│   │   ├── DeviceDataService.java         # 设备数据服务
│   │   └── MqttClientService.java         # MQTT客户端服务
│   └── MqttApplication.java               # 主启动类
├── src/main/resources/
│   ├── application.yml                    # 应用配置
│   └── static/index.html                  # Web测试页面
└── src/test/                              # 测试代码
```

## ⚙️ 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+

### 2. 克隆并运行

```bash
# 进入项目目录
cd springboot-netty-mqtt

# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### 3. 访问应用

- **Web测试页面**: http://localhost:8080
- **REST API状态**: http://localhost:8080/api/mqtt/status  
- **MQTT服务器**: tcp://localhost:1883

## 🔧 配置说明

### application.yml 主要配置

```yaml
# MQTT Broker配置
mqtt:
  broker:
    host: 0.0.0.0      # 服务器监听地址
    port: 1883         # MQTT端口
    boss-thread: 1     # Boss线程数
    worker-thread: 4   # Worker线程数

  # MQTT Client配置  
  client:
    server-url: tcp://localhost:1883
    client-id: springboot-mqtt-client
    username: admin
    password: password
    
    # 默认订阅主题
    subscribe-topics:
      - topic: "device/+/data"
        qos: 1
      - topic: "system/status"  
        qos: 0
```

## 📡 API接口

### 系统状态
```http
GET /api/mqtt/status
```

### 发布消息
```http
POST /api/mqtt/publish
Content-Type: application/x-www-form-urlencoded

topic=test/topic&payload=hello&qos=1
```

### 订阅主题
```http
POST /api/mqtt/subscribe
Content-Type: application/x-www-form-urlencoded

topic=device/+/data&qos=1
```

### 获取设备数据
```http
GET /api/mqtt/devices
```

### 发送模拟数据
```http
POST /api/mqtt/simulate
```

## 🔄 使用示例

### 1. 发布设备数据

```bash
curl -X POST "http://localhost:8080/api/mqtt/publish" \
  -d "topic=device/DEV001/data" \
  -d "payload={\"deviceId\":\"DEV001\",\"temperature\":25.5,\"humidity\":60}" \
  -d "qos=1"
```

### 2. 订阅主题

```bash
curl -X POST "http://localhost:8080/api/mqtt/subscribe" \
  -d "topic=device/+/data" \
  -d "qos=1"
```

### 3. 查看接收到的消息

```bash
curl http://localhost:8080/api/mqtt/messages
```

## 🏗️ 架构设计

### MQTT服务器架构
- 基于Netty构建高性能MQTT Broker
- 支持多客户端并发连接
- 实现完整的MQTT 3.1.1协议
- 支持QoS 0/1/2三种消息质量等级

### 客户端架构  
- 使用Eclipse Paho MQTT客户端
- 支持自动重连和心跳保持
- 消息缓存和过期清理机制
- 灵活的主题订阅管理

### 数据流程
1. 设备通过MQTT发布数据到Broker
2. 客户端订阅相关主题接收数据
3. 业务服务处理设备数据
4. REST API提供数据查询和控制接口
5. Web界面展示实时数据和状态

## 🔍 特性说明

### 自动数据模拟
应用启动后会自动每30秒发送模拟设备数据：
- 设备ID: DEV001, DEV002  
- 数据类型: 温度、湿度、电池电量
- 发布主题: `device/{deviceId}/data`

### 消息质量等级
- **QoS 0**: 最多发送一次，不保证送达
- **QoS 1**: 至少发送一次，保证送达  
- **QoS 2**: 仅发送一次，保证送达且不重复

### 主题通配符
- **+**: 单级通配符，如 `device/+/data`
- **#**: 多级通配符，如 `device/#`

## 🐛 故障排除

### 常见问题

1. **端口被占用**
   ```bash
   # 检查端口占用
   netstat -an | findstr :1883
   # 修改application.yml中的端口配置
   ```

2. **连接失败**
   - 检查防火墙设置
   - 确认MQTT服务器已启动
   - 验证用户名密码配置

3. **消息收不到**
   - 检查主题订阅是否成功
   - 确认QoS等级设置
   - 查看应用日志排查问题

### 日志查看
```bash
# 查看应用日志
tail -f logs/spring.log

# 开启DEBUG日志
# 在application.yml中设置
logging:
  level:
    com.example.mqtt: DEBUG
```

## 📈 扩展建议

1. **数据持久化**: 集成数据库存储设备数据
2. **用户认证**: 实现MQTT用户认证和权限控制
3. **集群部署**: 支持多节点MQTT Broker集群
4. **消息队列**: 集成RabbitMQ或Kafka处理大量消息
5. **监控告警**: 添加系统监控和异常告警功能

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🤝 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

---

**享受使用SpringBoot + Netty + MQTT！** 🎉