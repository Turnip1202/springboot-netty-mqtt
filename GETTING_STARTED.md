# SpringBoot Netty MQTT 项目使用指南

## 🎉 项目创建完成！

恭喜！你的SpringBoot + Netty + MQTT示例项目已经创建完成。这个项目包含了完整的MQTT服务器和客户端实现。

## 📁 项目文件概览

```
springboot-netty-mqtt/
├── pom.xml                                    # Maven项目配置
├── README.md                                  # 详细项目文档
├── Dockerfile                                 # Docker容器配置
├── docker-compose.yml                         # Docker Compose配置
├── start.bat                                  # Windows启动脚本
└── src/
    ├── main/
    │   ├── java/com/example/mqtt/
    │   │   ├── MqttApplication.java           # 主启动类
    │   │   ├── config/
    │   │   │   └── NettyMqttServerConfig.java # Netty MQTT服务器配置
    │   │   ├── controller/
    │   │   │   └── MqttController.java        # REST API控制器
    │   │   ├── handler/
    │   │   │   └── MqttMessageHandler.java    # MQTT消息处理器
    │   │   ├── model/
    │   │   │   ├── DeviceData.java            # 设备数据模型
    │   │   │   └── MqttMessage.java           # MQTT消息模型
    │   │   └── service/
    │   │       ├── DeviceDataService.java     # 设备数据服务
    │   │       └── MqttClientService.java     # MQTT客户端服务
    │   └── resources/
    │       ├── application.yml                # 应用配置文件
    │       └── static/
    │           └── index.html                 # Web测试界面
    └── test/
        └── java/com/example/mqtt/
            └── MqttClientServiceTest.java     # 测试用例
```

## 🚀 快速启动

### 方法1：使用启动脚本（推荐）
```bash
# Windows
start.bat

# Linux/Mac (需要先创建对应脚本)
./start.sh
```

### 方法2：使用Maven命令
```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 方法3：使用Docker
```bash
# 构建镜像
docker build -t springboot-mqtt .

# 使用Docker Compose启动
docker-compose up
```

## 📝 配置说明

### application.yml 主要配置项
- **MQTT Broker端口**: 1883 (默认)
- **HTTP服务端口**: 8080 (默认)
- **默认订阅主题**: device/+/data, system/status

### 如何修改配置
编辑 `src/main/resources/application.yml` 文件，修改相应配置项。

## 🔧 核心功能

### 1. Netty MQTT Broker
- 高性能MQTT服务器
- 支持多客户端并发连接
- 完整的MQTT 3.1.1协议实现
- 支持QoS 0/1/2消息质量等级

### 2. MQTT客户端
- 自动重连机制
- 心跳保持
- 消息缓存和过期清理
- 灵活的主题订阅管理

### 3. REST API接口
- `/api/mqtt/status` - 系统状态查询
- `/api/mqtt/publish` - 发布MQTT消息
- `/api/mqtt/subscribe` - 订阅主题
- `/api/mqtt/devices` - 查看设备数据
- 更多API请查看 MqttController.java

### 4. Web测试界面
访问 http://localhost:8080 使用Web界面测试MQTT功能。

### 5. 设备数据模拟
应用启动后自动每30秒发送模拟设备数据。

## 📊 使用示例

### 发布消息示例
```bash
curl -X POST "http://localhost:8080/api/mqtt/publish" \
  -d "topic=test/topic" \
  -d "payload=Hello MQTT" \
  -d "qos=1"
```

### 查看系统状态
```bash
curl http://localhost:8080/api/mqtt/status
```

### 查看设备数据
```bash
curl http://localhost:8080/api/mqtt/devices
```

## 🐛 常见问题解决

### 1. Maven依赖下载失败
- 检查网络连接
- 配置Maven镜像源（推荐使用阿里云镜像）
- 尝试使用IDE的离线模式

### 2. 端口冲突
- 修改application.yml中的端口配置
- 或者先停止占用端口的其他程序

### 3. MQTT连接失败
- 确认MQTT服务器已启动
- 检查防火墙设置
- 验证用户名密码配置

## 🔄 下一步扩展

1. **数据库集成**: 添加MySQL/PostgreSQL支持，持久化设备数据
2. **用户认证**: 实现JWT令牌认证和权限控制
3. **集群部署**: 支持多节点MQTT Broker集群
4. **监控告警**: 集成Micrometer和Prometheus监控
5. **消息队列**: 集成RabbitMQ/Kafka处理海量消息

## 📚 学习资源

- [MQTT协议规范](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html)
- [Netty官方文档](https://netty.io/wiki/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)

## 🎯 项目特色

✅ **完整性**: 包含服务器端和客户端完整实现  
✅ **实用性**: 提供Web界面和REST API，便于测试和集成  
✅ **可扩展性**: 模块化设计，易于扩展新功能  
✅ **生产就绪**: 包含配置管理、错误处理、日志记录等  
✅ **文档完善**: 详细的README和代码注释  

## 🎊 开始你的MQTT之旅！

现在你可以：
1. 启动应用 `mvn spring-boot:run`
2. 访问 http://localhost:8080 体验Web界面
3. 使用MQTT客户端工具连接到 tcp://localhost:1883
4. 通过REST API进行各种MQTT操作

享受使用SpringBoot + Netty + MQTT的乐趣！🚀