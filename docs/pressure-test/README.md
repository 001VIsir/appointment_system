# 压测方案（JMeter）

## 目标
覆盖注册/登录、任务查询、预约下单三类核心链路，输出 P95/P99、错误率与吞吐量。

## 前置准备
- 启动后端与 Redis/MySQL
- 准备账号与签名链接数据

## 文件说明
- `jmeter-plan.jmx`：JMeter 测试计划
- `data/users.csv`：测试用户数据
- `data/signed.csv`：签名链接数据
- `report-template.md`：压测报告模板

## 数据准备（自动生成签名链接/slotId）
```powershell
# 在 docs/pressure-test 目录下执行
./prepare-data.ps1 -BaseUrl "http://localhost:8080" -TasksCount 5
```

生成文件：
- `data/signed.csv`（签名链接）
- `data/slots.csv`

## 运行步骤（示例）
```bash
# 1) 进入目录
cd docs/pressure-test

# 2) 启动 JMeter GUI（编辑/校验）
# Windows 示例
jmeter.bat

# 3) 无界面执行（推荐）
# Windows 示例
jmeter.bat -n -t jmeter-plan.jmx -l results.jtl -e -o report
```

## 建议压测场景
1. 注册与登录混合场景（低占比）
2. 任务查询（中高占比）
3. 预约下单（中占比，重点看错误率与库存一致性）

## 输出指标
- 吞吐量（TPS）
- P95/P99 响应时间
- 错误率
- 限流命中率（HTTP 429）
