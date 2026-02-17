#!/bin/bash

# ============================================
# 预约系统 Docker 部署脚本 (WSL2)
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "========================================"
echo "  预约系统 Docker 部署助手"
echo "========================================"
echo ""

# 检查 Docker 是否运行
echo -e "${BLUE}[1/6] 检查 Docker 是否运行...${NC}"
if ! docker info &> /dev/null; then
    echo -e "${RED}[错误] Docker 未运行，请先启动 Docker Desktop${NC}"
    echo ""
    echo "解决方法："
    echo "1. 打开 Docker Desktop"
    echo "2. 确保 Docker Desktop 正在运行"
    echo "3. 重新运行此脚本"
    exit 1
fi
echo -e "${GREEN}[✓] Docker 运行正常${NC}"

# 检查 docker-compose
echo ""
echo -e "${BLUE}[2/6] 检查 docker-compose...${NC}"
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}[错误] docker-compose 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}[✓] docker-compose 已安装${NC}"

# 检查 .env 文件
echo ""
echo -e "${BLUE}[3/6] 检查环境变量配置...${NC}"
if [ ! -f .env ]; then
    echo -e "${YELLOW}[!] .env 文件不存在${NC}"

    if [ -f .env.docker ]; then
        echo -e "${BLUE}[i] 从 .env.docker 创建 .env 文件...${NC}"
        cp .env.docker .env
        echo -e "${YELLOW}[!] 已创建 .env 文件${NC}"
        echo ""
        echo -e "${RED}========================================${NC}"
        echo -e "${RED}  重要：请修改 .env 文件中的密码！${NC}"
        echo -e "${RED}========================================${NC}"
        echo ""
        echo "必须修改的配置："
        echo "  - DB_ROOT_PASSWORD"
        echo "  - DB_PASSWORD"
        echo "  - REDIS_PASSWORD"
        echo "  - RABBITMQ_PASSWORD"
        echo "  - SIGNED_LINK_SECRET"
        echo ""
        read -p "是否现在编辑 .env 文件？[Y/n] " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            ${EDITOR:-nano} .env
        else
            echo -e "${YELLOW}[!] 请手动编辑 .env 文件后再运行此脚本${NC}"
            exit 0
        fi
    elif [ -f .env.example ]; then
        echo -e "${BLUE}[i] 从 .env.example 创建 .env 文件...${NC}"
        cp .env.example .env
        echo -e "${YELLOW}[!] 已创建 .env 文件，请编辑修改密码后再运行此脚本${NC}"
        exit 0
    else
        echo -e "${RED}[错误] 找不到 .env.docker 或 .env.example 模板文件${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}[✓] .env 文件已存在${NC}"

    # 检查是否修改了默认密码
    if grep -q "YourStrongRootPassword123!" .env || grep -q "app123" .env; then
        echo -e "${YELLOW}[警告] 检测到 .env 中可能包含默认密码${NC}"
        read -p "是否继续？[y/N] " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
    fi
fi

# 询问部署模式
echo ""
echo -e "${BLUE}[4/6] 选择部署模式${NC}"
echo ""
echo "  1. 开发模式 (推荐用于本地开发，支持热重载)"
echo "  2. 生产模式 (用于实际部署)"
echo ""
read -p "请选择 [1/2]: " MODE

case $MODE in
    1)
        COMPOSE_FILE="docker-compose.yml"
        MODE_NAME="开发模式"
        ;;
    2)
        COMPOSE_FILE="docker-compose.prod.yml"
        MODE_NAME="生产模式"
        ;;
    *)
        echo -e "${RED}[错误] 无效的选择${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}[✓] 已选择: $MODE_NAME${NC}"

# 停止现有服务
echo ""
echo -e "${BLUE}[5/6] 停止现有服务（如果有）...${NC}"
if [ "$COMPOSE_FILE" = "docker-compose.yml" ]; then
    docker-compose down &> /dev/null || true
else
    docker-compose -f "$COMPOSE_FILE" down &> /dev/null || true
fi
echo -e "${GREEN}[✓] 已停止${NC}"

# 启动服务
echo ""
echo -e "${BLUE}[6/6] 启动服务...${NC}"
echo ""
echo -e "${YELLOW}正在构建和启动服务，请稍候...${NC}"
echo -e "${YELLOW}首次运行需要下载镜像，可能需要几分钟...${NC}"
echo ""

if [ "$COMPOSE_FILE" = "docker-compose.yml" ]; then
    if ! docker-compose up -d --build; then
        echo ""
        echo -e "${RED}[错误] 服务启动失败，请查看日志${NC}"
        echo "运行以下命令查看详细日志："
        echo "  docker-compose logs"
        exit 1
    fi
else
    if ! docker-compose -f "$COMPOSE_FILE" up -d --build; then
        echo ""
        echo -e "${RED}[错误] 服务启动失败，请查看日志${NC}"
        echo "运行以下命令查看详细日志："
        echo "  docker-compose -f $COMPOSE_FILE logs"
        exit 1
    fi
fi

# 等待服务就绪
echo ""
echo -e "${BLUE}[i] 等待服务就绪...${NC}"
sleep 5

# 显示成功信息
echo ""
echo "========================================"
echo -e "${GREEN}  部署成功！${NC}"
echo "========================================"
echo ""
echo "模式: $MODE_NAME"
echo ""
echo "访问地址:"
if [ "$MODE" = "1" ]; then
    echo -e "  ${GREEN}前端:${NC}     http://localhost:5173"
else
    echo -e "  ${GREEN}前端:${NC}     http://localhost"
fi
echo -e "  ${GREEN}后端:${NC}     http://localhost:8080"
echo -e "  ${GREEN}Swagger:${NC}  http://localhost:8080/swagger-ui.html"
echo -e "  ${GREEN}RabbitMQ:${NC} http://localhost:15672"
echo ""
echo "常用命令:"
if [ "$COMPOSE_FILE" = "docker-compose.yml" ]; then
    echo "  查看日志:   docker-compose logs -f"
    echo "  停止服务:   docker-compose down"
    echo "  重启服务:   docker-compose restart"
else
    echo "  查看日志:   docker-compose -f $COMPOSE_FILE logs -f"
    echo "  停止服务:   docker-compose -f $COMPOSE_FILE down"
    echo "  重启服务:   docker-compose -f $COMPOSE_FILE restart"
fi
echo ""

# 显示服务状态
echo "服务状态:"
echo ""
if [ "$COMPOSE_FILE" = "docker-compose.yml" ]; then
    docker-compose ps
else
    docker-compose -f "$COMPOSE_FILE" ps
fi

echo ""
echo -e "${GREEN}部署完成！${NC}"
echo ""

# 询问是否查看日志
read -p "是否查看实时日志？[Y/n] " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo ""
    echo -e "${BLUE}[i] 按 Ctrl+C 退出日志查看${NC}"
    echo ""
    sleep 2
    if [ "$COMPOSE_FILE" = "docker-compose.yml" ]; then
        docker-compose logs -f
    else
        docker-compose -f "$COMPOSE_FILE" logs -f
    fi
fi
