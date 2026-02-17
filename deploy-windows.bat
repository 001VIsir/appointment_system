@echo off
REM ============================================
REM 预约系统 Docker 部署脚本 (Windows)
REM ============================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   预约系统 Docker 部署助手
echo ========================================
echo.

REM 检查 Docker 是否运行
echo [1/5] 检查 Docker 是否运行...
docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)
echo [✓] Docker 运行正常

REM 检查 .env 文件
echo.
echo [2/5] 检查环境变量配置...
if not exist .env (
    echo [!] .env 文件不存在，从模板创建...
    if exist .env.docker (
        copy .env.docker .env >nul
        echo [!] 已创建 .env 文件，请编辑修改密码后再运行此脚本
        echo.
        echo 必须修改的配置：
        echo   - DB_ROOT_PASSWORD
        echo   - DB_PASSWORD
        echo   - REDIS_PASSWORD
        echo   - RABBITMQ_PASSWORD
        echo   - SIGNED_LINK_SECRET
        echo.
        notepad .env
        pause
        exit /b 0
    ) else if exist .env.example (
        copy .env.example .env >nul
        echo [!] 已创建 .env 文件，请编辑修改密码后再运行此脚本
        notepad .env
        pause
        exit /b 0
    ) else (
        echo [错误] 找不到 .env.docker 或 .env.example 模板文件
        pause
        exit /b 1
    )
)
echo [✓] .env 文件已存在

REM 询问部署模式
echo.
echo [3/5] 选择部署模式
echo.
echo   1. 开发模式 (推荐用于本地开发，支持热重载)
echo   2. 生产模式 (用于实际部署)
echo.
set /p MODE="请选择 [1/2]: "

if "%MODE%"=="1" (
    set COMPOSE_FILE=docker-compose.yml
    set MODE_NAME=开发模式
) else if "%MODE%"=="2" (
    set COMPOSE_FILE=docker-compose.prod.yml
    set MODE_NAME=生产模式
) else (
    echo [错误] 无效的选择
    pause
    exit /b 1
)

echo.
echo [✓] 已选择: %MODE_NAME%

REM 停止现有服务
echo.
echo [4/5] 停止现有服务（如果有）...
if "%COMPOSE_FILE%"=="docker-compose.yml" (
    docker-compose down >nul 2>&1
) else (
    docker-compose -f %COMPOSE_FILE% down >nul 2>&1
)
echo [✓] 已停止

REM 启动服务
echo.
echo [5/5] 启动服务...
echo.
echo 正在构建和启动服务，请稍候...
echo 首次运行需要下载镜像，可能需要几分钟...
echo.

if "%COMPOSE_FILE%"=="docker-compose.yml" (
    docker-compose up -d --build
) else (
    docker-compose -f %COMPOSE_FILE% up -d --build
)

if errorlevel 1 (
    echo.
    echo [错误] 服务启动失败，请查看日志
    echo 运行以下命令查看详细日志：
    if "%COMPOSE_FILE%"=="docker-compose.yml" (
        echo   docker-compose logs
    ) else (
        echo   docker-compose -f %COMPOSE_FILE% logs
    )
    pause
    exit /b 1
)

echo.
echo ========================================
echo   部署成功！
echo ========================================
echo.
echo 模式: %MODE_NAME%
echo.
echo 访问地址:
if "%MODE%"=="1" (
    echo   前端:     http://localhost:5173
) else (
    echo   前端:     http://localhost
)
echo   后端:     http://localhost:8080
echo   Swagger:  http://localhost:8080/swagger-ui.html
echo   RabbitMQ: http://localhost:15672
echo.
echo 常用命令:
if "%COMPOSE_FILE%"=="docker-compose.yml" (
    echo   查看日志:   docker-compose logs -f
    echo   停止服务:   docker-compose down
    echo   重启服务:   docker-compose restart
) else (
    echo   查看日志:   docker-compose -f %COMPOSE_FILE% logs -f
    echo   停止服务:   docker-compose -f %COMPOSE_FILE% down
    echo   重启服务:   docker-compose -f %COMPOSE_FILE% restart
)
echo.
echo 按任意键查看服务状态...
pause >nul

REM 显示服务状态
echo.
echo 服务状态:
echo.
if "%COMPOSE_FILE%"=="docker-compose.yml" (
    docker-compose ps
) else (
    docker-compose -f %COMPOSE_FILE% ps
)

echo.
echo 部署完成！
pause
