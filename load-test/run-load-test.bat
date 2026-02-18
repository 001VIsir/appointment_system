@echo off
REM =====================================================
REM 压力测试运行脚本 (Windows)
REM FEAT-036: 压力测试
REM =====================================================

setlocal EnableDelayedExpansion

REM 默认参数
if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080
if "%THREAD_COUNT%"=="" set THREAD_COUNT=50
if "%RAMP_UP%"=="" set RAMP_UP=10
if "%LOOP_COUNT%"=="" set LOOP_COUNT=10

REM 获取时间戳
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set YYYY=%dt:~0,4%
set MM=%dt:~4,2%
set DD=%dt:~6,2%
set HH=%dt:~8,2%
set Min=%dt:~10,2%
set SS=%dt:~12,2%
set TIMESTAMP=%YYYY%%MM%%DD%_%HH%%Min%%SS%

if "%REPORT_DIR%"=="" set REPORT_DIR=load-test-report-%TIMESTAMP%

REM 脚本目录
set SCRIPT_DIR=%~dp0
set TEST_PLAN=%SCRIPT_DIR%appointment-system-load-test.jmx

REM 颜色代码 (Windows 10+ 支持)
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

echo.
echo %BLUE%========================================%NC%
echo %BLUE%预约系统压力测试脚本 (Windows)%NC%
echo %BLUE%========================================%NC%
echo.

REM 检查参数
:parse_args
if "%~1"=="" goto :check_jmeter
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--help" goto :show_help
if /i "%~1"=="-u" (set BASE_URL=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="--url" (set BASE_URL=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="-t" (set THREAD_COUNT=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="--threads" (set THREAD_COUNT=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="-r" (set RAMP_UP=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="--rampup" (set RAMP_UP=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="-l" (set LOOP_COUNT=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="--loops" (set LOOP_COUNT=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="-o" (set REPORT_DIR=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="--output" (set REPORT_DIR=%~2& shift& shift& goto :parse_args)
if /i "%~1"=="-g" (set GUI_MODE=1& shift& goto :parse_args)
if /i "%~1"=="--gui" (set GUI_MODE=1& shift& goto :parse_args)
echo %RED%未知参数: %~1%NC%
goto :show_help

:show_help
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   -h, --help           显示帮助信息
echo   -u, --url URL        后端服务地址 (默认: %BASE_URL%)
echo   -t, --threads NUM    并发线程数 (默认: %THREAD_COUNT%)
echo   -r, --rampup NUM     启动时间/秒 (默认: %RAMP_UP%)
echo   -l, --loops NUM      循环次数 (默认: %LOOP_COUNT%)
echo   -o, --output DIR     报告输出目录 (默认: %REPORT_DIR%)
echo   -g, --gui            使用 GUI 模式运行
echo.
echo 示例:
echo   %~nx0                              # 使用默认参数运行
echo   %~nx0 -t 100 -r 30                 # 100线程，30秒启动
echo   %~nx0 -u http://192.168.1.100:8080 # 指定服务地址
echo   %~nx0 -g                           # GUI 模式
exit /b 0

:check_jmeter
REM 检查 JMeter
where jmeter >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set JMETER_CMD=jmeter
    goto :jmeter_found
)

if defined JMETER_HOME (
    set JMETER_CMD=%JMETER_HOME%\bin\jmeter.bat
    goto :jmeter_found
)

echo %RED%错误: 未找到 JMeter%NC%
echo 请安装 JMeter 或设置 JMETER_HOME 环境变量
echo 下载地址: https://jmeter.apache.org/download_jmeter.cgi
exit /b 1

:jmeter_found
echo %GREEN%JMeter: %JMETER_CMD%%NC%
echo.

REM GUI 模式
if defined GUI_MODE (
    echo %BLUE%启动 JMeter GUI...%NC%
    %JMETER_CMD% -t "%TEST_PLAN%" ^
        -JBASE_URL="%BASE_URL%" ^
        -JTHREAD_COUNT="%THREAD_COUNT%" ^
        -JRAMP_UP="%RAMP_UP%" ^
        -JLOOP_COUNT="%LOOP_COUNT%"
    exit /b 0
)

REM 命令行模式
echo 服务地址: %YELLOW%%BASE_URL%%NC%
echo 并发线程: %YELLOW%%THREAD_COUNT%%NC%
echo 启动时间: %YELLOW%%RAMP_UP% 秒%NC%
echo 循环次数: %YELLOW%%LOOP_COUNT%%NC%
echo 报告目录: %YELLOW%%REPORT_DIR%%NC%
echo.

REM 检查服务可用性
echo %BLUE%检查服务可用性...%NC%
curl -s --connect-timeout 5 "%BASE_URL%/actuator/health" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo %GREEN%服务正常%NC%
) else (
    echo %YELLOW%警告: 无法连接到服务，请确认服务已启动%NC%
    set /p continue_test="是否继续? (y/N): "
    if /i "!continue_test!" neq "y" exit /b 0
)

REM 创建报告目录
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%"

REM 运行测试
echo.
echo %BLUE%开始压力测试...%NC%
echo.

%JMETER_CMD% -n -t "%TEST_PLAN%" ^
    -JBASE_URL="%BASE_URL%" ^
    -JTHREAD_COUNT="%THREAD_COUNT%" ^
    -JRAMP_UP="%RAMP_UP%" ^
    -JLOOP_COUNT="%LOOP_COUNT%" ^
    -l "%REPORT_DIR%\results.jtl" ^
    -e -o "%REPORT_DIR%\html-report"

echo.
echo %GREEN%========================================%NC%
echo %GREEN%测试完成!%NC%
echo %GREEN%========================================%NC%
echo.
echo 结果文件: %YELLOW%%REPORT_DIR%\results.jtl%NC%
echo HTML 报告: %YELLOW%%REPORT_DIR%\html-report\index.html%NC%
echo.

REM 询问是否打开报告
set /p open_report="是否打开报告? (y/N): "
if /i "%open_report%"=="y" (
    start "" "%REPORT_DIR%\html-report\index.html"
)

endlocal
