@echo off
echo ========================================
echo SpringBoot Netty MQTT 示例项目启动脚本
echo ========================================

echo 正在编译项目...
call mvn clean compile

if %ERRORLEVEL% == 0 (
    echo 编译成功！正在启动应用...
    call mvn spring-boot:run
) else (
    echo 编译失败，请检查网络连接和Maven配置
    echo.
    echo 如果网络问题导致依赖下载失败，你可以：
    echo 1. 检查网络连接
    echo 2. 配置Maven镜像源
    echo 3. 使用离线方式构建
    echo.
    pause
)