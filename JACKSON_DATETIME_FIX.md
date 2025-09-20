# Jackson LocalDateTime 序列化问题解决方案

## 问题描述

在运行应用时出现了Jackson无法序列化Java 8 `LocalDateTime` 类型的错误：

```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling 
(through reference chain: com.example.mqtt.model.DeviceData["timestamp"])
```

## 问题原因

1. **缺少JSR310模块**：Jackson默认不支持Java 8的时间类型
2. **ObjectMapper配置不完整**：未注册JavaTimeModule
3. **时间序列化格式问题**：默认将时间序列化为时间戳

## 解决方案

### 1. 添加Jackson JSR310依赖

在 `pom.xml` 中添加：

```xml
<!-- Jackson JSR310模块，支持Java 8时间类型 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### 2. 配置ObjectMapper支持Java 8时间类型

#### A. 在MqttClientService中：

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public MqttClientService() {
    // 配置ObjectMapper支持Java 8时间类型
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```

#### B. 在DeviceDataService中：

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public DeviceDataService(MqttClientService mqttClientService) {
    this.mqttClientService = mqttClientService;
    // 配置ObjectMapper支持Java 8时间类型
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // 启动模拟数据发送任务
    startSimulationTask();
}
```

### 3. 全局Jackson配置

在 `application.yml` 中添加全局配置：

```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    # 支持Java 8时间类型
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      adjust-dates-to-context-time-zone: false
```

## 配置说明

### JavaTimeModule作用
- 提供Java 8时间类型的序列化/反序列化支持
- 支持LocalDateTime、LocalDate、LocalTime等类型

### 关键配置项

1. **write-dates-as-timestamps: false**
   - 将时间序列化为ISO-8601格式字符串而不是时间戳
   - 提高可读性

2. **adjust-dates-to-context-time-zone: false**
   - 保持原始时区信息
   - 避免时区转换问题

## 序列化效果

### 修复前（❌ 错误）
```
序列化失败，抛出异常
```

### 修复后（✅ 正确）
```json
{
  "deviceId": "DEV001",
  "deviceName": "温湿度传感器",
  "timestamp": "2025-09-20T14:52:42",
  "temperature": 25.5,
  "humidity": 60.0
}
```

## 验证方法

1. **运行应用**：启动SpringBoot应用
2. **触发数据发送**：等待自动模拟数据发送或手动触发
3. **查看日志**：确认没有序列化错误
4. **检查Web界面**：访问 http://localhost:8080 查看设备数据

## 最佳实践

### 1. 统一时间处理
```java
// 推荐：使用LocalDateTime
private LocalDateTime timestamp;

// 避免：混用不同时间类型
private Date timestamp; // 不推荐
```

### 2. 时间格式注解
```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime timestamp;
```

### 3. 全局配置优先
- 优先使用application.yml全局配置
- 减少重复代码
- 保持一致性

### 4. 依赖管理
```xml
<!-- Spring Boot已包含jackson-databind -->
<!-- 只需添加JSR310模块 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

## 相关时间类型支持

修复后支持的Java 8时间类型：
- `LocalDateTime` - 本地日期时间
- `LocalDate` - 本地日期
- `LocalTime` - 本地时间
- `ZonedDateTime` - 带时区的日期时间
- `Instant` - 时间戳
- `Duration` - 时间间隔
- `Period` - 日期间隔

这个解决方案确保了Java 8时间类型的正确序列化，提高了应用的稳定性和数据交换的准确性。