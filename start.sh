#!/bin/bash

echo "========================================"
echo "SpringBoot Netty MQTT 示例项目启动脚本"
echo "========================================"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装JDK 17或更高版本"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven 3.6+或使用IDE运行"
    exit 1
fi

echo "正在编译项目..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "编译成功！正在启动应用..."
    echo "访问地址: http://localhost:8080"
    echo "MQTT服务器: tcp://localhost:1883"
    echo "========================================"
    mvn spring-boot:run
else
    echo "编译失败，请检查以下问题："
    echo "1. 网络连接是否正常"
    echo "2. Maven镜像源配置"
    echo "3. JDK版本是否为17+"
    echo ""
    echo "解决方案："
    echo "- 配置Maven阿里云镜像源"
    echo "- 使用IDE的Maven工具"
    echo "- 检查防火墙设置"
    exit 1
fi