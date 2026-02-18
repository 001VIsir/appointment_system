#!/bin/bash
# =====================================================
# 压力测试运行脚本 (Linux/Mac)
# FEAT-036: 压力测试
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认参数
BASE_URL="${BASE_URL:-http://localhost:8080}"
THREAD_COUNT="${THREAD_COUNT:-50}"
RAMP_UP="${RAMP_UP:-10}"
LOOP_COUNT="${LOOP_COUNT:-10}"
REPORT_DIR="${REPORT_DIR:-load-test-report-$(date +%Y%m%d_%H%M%S)}"

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TEST_PLAN="$SCRIPT_DIR/appointment-system-load-test.jmx"

# 打印帮助
print_help() {
    echo -e "${BLUE}预约系统压力测试脚本${NC}"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help           显示帮助信息"
    echo "  -u, --url URL        后端服务地址 (默认: $BASE_URL)"
    echo "  -t, --threads NUM    并发线程数 (默认: $THREAD_COUNT)"
    echo "  -r, --rampup NUM     启动时间/秒 (默认: $RAMP_UP)"
    echo "  -l, --loops NUM      循环次数 (默认: $LOOP_COUNT)"
    echo "  -o, --output DIR     报告输出目录 (默认: $REPORT_DIR)"
    echo "  -g, --gui            使用 GUI 模式运行"
    echo "  -p, --prepare        准备测试数据"
    echo ""
    echo "示例:"
    echo "  $0                              # 使用默认参数运行"
    echo "  $0 -t 100 -r 30                 # 100线程，30秒启动"
    echo "  $0 -u http://192.168.1.100:8080 # 指定服务地址"
    echo "  $0 -g                           # GUI 模式"
}

# 检查 JMeter
check_jmeter() {
    if command -v jmeter &> /dev/null; then
        JMETER_CMD="jmeter"
    elif [ -n "$JMETER_HOME" ]; then
        JMETER_CMD="$JMETER_HOME/bin/jmeter"
    else
        echo -e "${RED}错误: 未找到 JMeter${NC}"
        echo "请安装 JMeter 或设置 JMETER_HOME 环境变量"
        echo "下载地址: https://jmeter.apache.org/download_jmeter.cgi"
        exit 1
    fi
    echo -e "${GREEN}JMeter: $JMETER_CMD${NC}"
}

# 准备测试数据
prepare_data() {
    echo -e "${BLUE}准备测试数据...${NC}"

    # 检查 MySQL 连接
    if [ -z "$MYSQL_HOST" ]; then
        MYSQL_HOST="localhost"
    fi
    if [ -z "$MYSQL_USER" ]; then
        MYSQL_USER="root"
    fi

    echo "MySQL 主机: $MYSQL_HOST"
    echo "MySQL 用户: $MYSQL_USER"

    read -sp "请输入 MySQL 密码: " MYSQL_PASSWORD
    echo ""

    mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" appointment_system < "$SCRIPT_DIR/prepare-test-data.sql"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}测试数据准备完成${NC}"
    else
        echo -e "${RED}测试数据准备失败${NC}"
        exit 1
    fi
}

# 运行 GUI 模式
run_gui() {
    echo -e "${BLUE}启动 JMeter GUI...${NC}"
    $JMETER_CMD -t "$TEST_PLAN" \
        -JBASE_URL="$BASE_URL" \
        -JTHREAD_COUNT="$THREAD_COUNT" \
        -JRAMP_UP="$RAMP_UP" \
        -JLOOP_COUNT="$LOOP_COUNT"
}

# 运行命令行模式
run_cli() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}预约系统压力测试${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "服务地址: ${YELLOW}$BASE_URL${NC}"
    echo -e "并发线程: ${YELLOW}$THREAD_COUNT${NC}"
    echo -e "启动时间: ${YELLOW}$RAMP_UP 秒${NC}"
    echo -e "循环次数: ${YELLOW}$LOOP_COUNT${NC}"
    echo -e "报告目录: ${YELLOW}$REPORT_DIR${NC}"
    echo ""

    # 检查服务是否可用
    echo -e "${BLUE}检查服务可用性...${NC}"
    if curl -s --connect-timeout 5 "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}服务正常${NC}"
    else
        echo -e "${YELLOW}警告: 无法连接到服务，请确认服务已启动${NC}"
        read -p "是否继续? (y/N): " continue_test
        if [ "$continue_test" != "y" ] && [ "$continue_test" != "Y" ]; then
            exit 0
        fi
    fi

    # 创建报告目录
    mkdir -p "$REPORT_DIR"

    # 运行测试
    echo -e "${BLUE}开始压力测试...${NC}"
    echo ""

    $JMETER_CMD -n -t "$TEST_PLAN" \
        -JBASE_URL="$BASE_URL" \
        -JTHREAD_COUNT="$THREAD_COUNT" \
        -JRAMP_UP="$RAMP_UP" \
        -JLOOP_COUNT="$LOOP_COUNT" \
        -l "$REPORT_DIR/results.jtl" \
        -e -o "$REPORT_DIR/html-report"

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}测试完成!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "结果文件: ${YELLOW}$REPORT_DIR/results.jtl${NC}"
    echo -e "HTML 报告: ${YELLOW}$REPORT_DIR/html-report/index.html${NC}"
    echo ""

    # 如果在 WSL 中，尝试打开 Windows 浏览器
    if command -v explorer.exe &> /dev/null; then
        read -p "是否打开报告? (y/N): " open_report
        if [ "$open_report" = "y" ] || [ "$open_report" = "Y" ]; then
            REPORT_PATH="$(wslpath -w "$(realpath "$REPORT_DIR/html-report/index.html")")"
            explorer.exe "$REPORT_PATH"
        fi
    fi
}

# 解析参数
GUI_MODE=false
PREPARE_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            print_help
            exit 0
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        -t|--threads)
            THREAD_COUNT="$2"
            shift 2
            ;;
        -r|--rampup)
            RAMP_UP="$2"
            shift 2
            ;;
        -l|--loops)
            LOOP_COUNT="$2"
            shift 2
            ;;
        -o|--output)
            REPORT_DIR="$2"
            shift 2
            ;;
        -g|--gui)
            GUI_MODE=true
            shift
            ;;
        -p|--prepare)
            PREPARE_MODE=true
            shift
            ;;
        *)
            echo -e "${RED}未知参数: $1${NC}"
            print_help
            exit 1
            ;;
    esac
done

# 主流程
check_jmeter

if [ "$PREPARE_MODE" = true ]; then
    prepare_data
    exit 0
fi

if [ "$GUI_MODE" = true ]; then
    run_gui
else
    run_cli
fi
