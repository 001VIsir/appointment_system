# 项目技术增强计划

> 基于 sky-take-out（苍穹外卖）项目的优秀实践，为预约系统规划技术增强与商业价值提升方案。

## 一、现状对比分析

### 1.1 当前项目已有亮点

| 技术点 | 实现状态 | 说明 |
|--------|----------|------|
| Spring Boot 4 + Java 21 | ✅ 已实现 | 最新LTS版本 |
| Spring Security + Redis Session | ✅ 已实现 | 分布式会话 |
| 乐观锁并发控制 | ✅ 已实现 | 防超卖 |
| 短期签名链接 | ✅ 已实现 | 安全外部访问 |
| Redis 限流 | ✅ 已实现 | 429返回 |
| Prometheus 监控 | ✅ 已实现 | 指标输出 |
| Flyway 数据库迁移 | ✅ 已实现 | 版本控制 |

### 1.2 sky-take-out 可学习的技术点

| 技术点 | sky-take-out 实现 | 预约系统现状 | 建议优先级 |
|--------|-------------------|--------------|------------|
| **WebSocket 实时通信** | 来单提醒、客户催单 | ❌ 未实现 | ⭐⭐⭐⭐⭐ 高 |
| **Spring Task 定时任务** | 超时订单自动取消 | ❌ 未实现 | ⭐⭐⭐⭐⭐ 高 |
| **AOP 公共字段填充** | 自动填充创建/更新时间 | ❌ 手动填充 | ⭐⭐⭐⭐ 高 |
| **Spring Cache 声明式缓存** | 菜品缓存+失效策略 | ❌ 仅限流 | ⭐⭐⭐⭐ 高 |
| **数据统计报表** | 营业额/用户/订单统计 | ⚠️ 基础统计 | ⭐⭐⭐⭐ 高 |
| **Excel 报表导出** | Apache POI 导出 | ❌ 未实现 | ⭐⭐⭐ 中 |
| **微信小程序登录** | 微信OAuth | ❌ 未实现 | ⭐⭐⭐ 中 |

---

## 二、技术增强方案

### 2.1 WebSocket 实时通知系统（优先级：⭐⭐⭐⭐⭐）

**学习来源**：sky-take-out 的来单提醒和客户催单功能

**应用场景**：
- 商户端：新预约通知、预约取消通知、预约提醒
- 用户端：预约确认通知、预约即将开始提醒

**实现方案**：
```
┌─────────────┐    WebSocket    ┌─────────────┐
│   商户端    │◀──────────────▶│             │
└─────────────┘                 │   Spring    │
                                │  WebSocket  │
┌─────────────┐    WebSocket    │   Server    │
│   用户端    │◀──────────────▶│             │
└─────────────┘                 └─────────────┘
```

**技术实现**：
```java
@Component
@ServerEndpoint("/ws/{userId}")
public class AppointmentWebSocketServer {
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        sessionMap.put(userId, session);
    }

    // 发送预约通知给商户
    public void notifyMerchant(Long merchantId, String message) {
        Session session = sessionMap.get("merchant_" + merchantId);
        if (session != null) {
            session.getBasicRemote().sendText(message);
        }
    }

    // 发送预约提醒给用户
    public void notifyUser(Long userId, String message) {
        Session session = sessionMap.get("user_" + userId);
        if (session != null) {
            session.getBasicRemote().sendText(message);
        }
    }
}
```

**面试亮点**：
- 理解 WebSocket 与 HTTP 轮询的区别
- 掌握 WebSocket 在 Spring Boot 中的集成
- 了解 WebSocket 集群方案（Redis Pub/Sub）

---

### 2.2 Spring Task 定时任务（优先级：⭐⭐⭐⭐⭐）

**学习来源**：sky-take-out 的订单超时自动取消

**应用场景**：
1. **预约超时自动取消**：用户预约后X分钟未确认，自动取消
2. **已结束预约自动完成**：预约时间过后，自动标记为已完成
3. **预约提前提醒**：预约开始前30分钟，发送提醒通知
4. **每日数据汇总**：每日凌晨统计昨日数据

**实现方案**：
```java
@Component
@Slf4j
public class AppointmentTask {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AppointmentWebSocketServer webSocketServer;

    /**
     * 处理超时未确认的预约（每分钟执行）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutBookings() {
        log.info("开始处理超时预约...");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);

        List<Booking> timeoutBookings = bookingRepository
            .findByStatusAndCreatedAtBefore(BookingStatus.PENDING, timeoutThreshold);

        timeoutBookings.forEach(booking -> {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelReason("超时未确认，自动取消");
            bookingRepository.save(booking);

            // 通知用户
            webSocketServer.notifyUser(booking.getUserId(),
                "{\"type\":\"BOOKING_CANCELLED\",\"message\":\"预约已超时取消\"}");
        });
    }

    /**
     * 预约开始前30分钟提醒（每5分钟执行）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void remindUpcomingBookings() {
        LocalDateTime reminderStart = LocalDateTime.now().plusMinutes(25);
        LocalDateTime reminderEnd = LocalDateTime.now().plusMinutes(35);

        List<Booking> upcomingBookings = bookingRepository
            .findUpcomingBookings(reminderStart, reminderEnd);

        upcomingBookings.forEach(booking -> {
            webSocketServer.notifyUser(booking.getUserId(),
                "{\"type\":\"BOOKING_REMINDER\",\"message\":\"您的预约即将开始\"}");
        });
    }

    /**
     * 自动完成已结束的预约（每小时执行）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCompleteBookings() {
        List<Booking> completedBookings = bookingRepository
            .findByStatusAndSlotEndTimeBefore(BookingStatus.CONFIRMED, LocalDateTime.now());

        completedBookings.forEach(booking -> {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        });
    }
}
```

**面试亮点**：
- 理解 Cron 表达式
- 掌握 Spring Task 的使用
- 了解分布式定时任务（XXL-JOB）的演进

---

### 2.3 AOP 公共字段自动填充（优先级：⭐⭐⭐⭐）

**学习来源**：sky-take-out 的 AutoFillAspect

**应用场景**：
- 自动填充 `createdAt`、`createdBy`、`updatedAt`、`updatedBy` 字段
- 减少重复代码，保证数据一致性

**实现方案**：

1. **自定义注解**：
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value();
}

public enum OperationType {
    INSERT, UPDATE
}
```

2. **AOP 切面**：
```java
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    @Pointcut("execution(* org.example.appointment_system.repository.*.*(..)) " +
              "&& @annotation(org.example.appointment_system.annotation.AutoFill)")
    public void autoFillPointCut() {}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;

        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = SecurityUtils.getCurrentUserId();

        try {
            if (operationType == OperationType.INSERT) {
                Method setCreatedAt = entity.getClass()
                    .getDeclaredMethod("setCreatedAt", LocalDateTime.class);
                Method setCreatedBy = entity.getClass()
                    .getDeclaredMethod("setCreatedBy", Long.class);
                Method setUpdatedAt = entity.getClass()
                    .getDeclaredMethod("setUpdatedAt", LocalDateTime.class);
                Method setUpdatedBy = entity.getClass()
                    .getDeclaredMethod("setUpdatedBy", Long.class);

                setCreatedAt.invoke(entity, now);
                setCreatedBy.invoke(entity, currentUserId);
                setUpdatedAt.invoke(entity, now);
                setUpdatedBy.invoke(entity, currentUserId);
            } else if (operationType == OperationType.UPDATE) {
                Method setUpdatedAt = entity.getClass()
                    .getDeclaredMethod("setUpdatedAt", LocalDateTime.class);
                Method setUpdatedBy = entity.getClass()
                    .getDeclaredMethod("setUpdatedBy", Long.class);

                setUpdatedAt.invoke(entity, now);
                setUpdatedBy.invoke(entity, currentUserId);
            }
        } catch (Exception e) {
            log.error("公共字段自动填充失败", e);
        }
    }
}
```

3. **使用示例**：
```java
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @AutoFill(OperationType.INSERT)
    Booking save(Booking booking);
}
```

**面试亮点**：
- 深入理解 AOP 原理（动态代理）
- 掌握 Java 反射机制
- 了解切面编程的实际应用场景

---

### 2.4 Spring Cache 声明式缓存（优先级：⭐⭐⭐⭐）

**学习来源**：sky-take-out 的菜品缓存策略

**应用场景**：
- 商户信息缓存（5分钟）
- 服务项列表缓存（30秒）
- 热门预约任务缓存（1分钟）

**实现方案**：

1. **启用缓存**：
```java
@SpringBootApplication
@EnableCaching  // 启用缓存
public class AppointmentSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentSystemApplication.class, args);
    }
}
```

2. **缓存配置**：
```java
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("merchant",
                config.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("serviceItem",
                config.entryTtl(Duration.ofSeconds(30)))
            .withCacheConfiguration("task",
                config.entryTtl(Duration.ofMinutes(1)))
            .build();
    }
}
```

3. **使用示例**：
```java
@Service
public class MerchantService {

    // 查询时缓存
    @Cacheable(value = "merchant", key = "#merchantId")
    public MerchantProfile getMerchantById(Long merchantId) {
        return merchantRepository.findById(merchantId)
            .orElseThrow(() -> new BusinessException("商户不存在"));
    }

    // 更新时清除缓存
    @CacheEvict(value = "merchant", key = "#merchantId")
    public void updateMerchant(Long merchantId, MerchantProfile profile) {
        // ...
    }

    // 批量清除缓存
    @CacheEvict(value = "serviceItem", allEntries = true)
    public void clearServiceItemCache() {
        log.info("清除服务项缓存");
    }
}
```

**缓存策略设计**：
```
┌──────────────────────────────────────────────────────────────┐
│                     缓存策略矩阵                               │
├──────────────┬────────────┬──────────────┬───────────────────┤
│    数据类型   │  缓存时间   │   缓存策略    │     失效触发      │
├──────────────┼────────────┼──────────────┼───────────────────┤
│   商户信息    │   5 分钟    │  Cache-Aside │  商户信息更新时    │
│   服务项列表  │   30 秒     │  Cache-Aside │  新增/修改服务项   │
│   热门任务    │   1 分钟    │  Cache-Aside │  任务状态变更时    │
│   可用时段    │   不缓存    │  实时查询    │  -                │
└──────────────┴────────────┴──────────────┴───────────────────┘
```

**面试亮点**：
- 理解缓存穿透、缓存击穿、缓存雪崩
- 掌握 Cache-Aside、Write-Through 等缓存策略
- 了解分布式缓存一致性方案

---

### 2.5 数据统计报表增强（优先级：⭐⭐⭐⭐）

**学习来源**：sky-take-out 的营业额统计、用户统计、订单统计

**新增统计维度**：

#### 2.5.1 营业额统计（付费预约场景）
```java
public class TurnoverReportVO {
    private String dateList;        // 日期列表，逗号分隔
    private String turnoverList;    // 营业额列表，逗号分隔
}

// Service 实现
public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
    List<LocalDate> dateList = getDateList(begin, end);
    List<BigDecimal> turnoverList = new ArrayList<>();

    for (LocalDate date : dateList) {
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

        BigDecimal turnover = bookingRepository.sumCompletedBookingsByDate(
            startTime, endTime, BookingStatus.COMPLETED);
        turnoverList.add(turnover != null ? turnover : BigDecimal.ZERO);
    }

    return TurnoverReportVO.builder()
        .dateList(StringUtils.join(dateList, ","))
        .turnoverList(StringUtils.join(turnoverList, ","))
        .build();
}
```

#### 2.5.2 预约转化率统计
```java
public class BookingConversionVO {
    private String dateList;
    private String totalList;       // 总预约数
    private String completedList;   // 完成数
    private String cancelledList;   // 取消数
    private Double conversionRate;  // 转化率
}
```

#### 2.5.3 热门服务排行
```java
public class ServiceRankingVO {
    private String nameList;        // 服务名称列表
    private String countList;       // 预约数量列表
}

public ServiceRankingVO getTop10Services(LocalDate begin, LocalDate end) {
    List<ServiceRankingDTO> rankings = bookingRepository
        .findTop10Services(begin, end);

    return ServiceRankingVO.builder()
        .nameList(StringUtils.join(rankings.stream()
            .map(ServiceRankingDTO::getName).toList(), ","))
        .countList(StringUtils.join(rankings.stream()
            .map(ServiceRankingDTO::getCount).toList(), ","))
        .build();
}
```

#### 2.5.4 用户增长趋势
```java
public class UserGrowthVO {
    private String dateList;
    private String newUserList;     // 新增用户
    private String totalUserList;   // 累计用户
}
```

**前端可视化（ECharts）**：
```javascript
// 营业额趋势图
option = {
  title: { text: '营业额趋势' },
  xAxis: { type: 'category', data: dateList.split(',') },
  yAxis: { type: 'value' },
  series: [{ data: turnoverList.split(','), type: 'line', smooth: true }]
};
```

---

### 2.6 Excel 报表导出（优先级：⭐⭐⭐）

**学习来源**：sky-take-out 的运营数据报表导出

**应用场景**：
- 预约数据导出（管理员/商户）
- 运营报表导出（月度/季度）
- 用户数据导出

**实现方案**：

1. **依赖添加**：
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

2. **导出服务**：
```java
@Service
public class ReportExportService {

    public void exportBookingReport(LocalDate begin, LocalDate end,
                                    HttpServletResponse response) {
        try {
            // 1. 查询数据
            List<BookingReportDTO> dataList = bookingRepository
                .findBookingReport(begin, end);

            // 2. 创建 Excel
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("预约数据报表");

            // 3. 创建表头
            XSSFRow headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("日期");
            headerRow.createCell(1).setCellValue("预约数");
            headerRow.createCell(2).setCellValue("完成数");
            headerRow.createCell(3).setCellValue("取消数");
            headerRow.createCell(4).setCellValue("营业额");

            // 4. 填充数据
            for (int i = 0; i < dataList.size(); i++) {
                BookingReportDTO data = dataList.get(i);
                XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(data.getDate().toString());
                row.createCell(1).setCellValue(data.getTotalCount());
                row.createCell(2).setCellValue(data.getCompletedCount());
                row.createCell(3).setCellValue(data.getCancelledCount());
                row.createCell(4).setCellValue(data.getTurnover().doubleValue());
            }

            // 5. 输出到响应流
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                "attachment;filename=booking_report.xlsx");

            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            out.close();
            workbook.close();

        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }
}
```

3. **Controller 接口**：
```java
@GetMapping("/export")
@ApiOperation("导出预约数据报表")
public void exportReport(
    @RequestParam LocalDate begin,
    @RequestParam LocalDate end,
    HttpServletResponse response) {
    reportExportService.exportBookingReport(begin, end, response);
}
```

---

## 三、商业价值增强方案

### 3.1 商户营业状态管理

**功能描述**：支持商户设置营业/暂停营业状态

**业务价值**：
- 商户可灵活控制预约开放时间
- 紧急情况可快速暂停预约

**数据模型**：
```sql
ALTER TABLE merchant_profiles ADD COLUMN business_status TINYINT DEFAULT 1;
-- 1: 营业中, 0: 休息中
```

**实现要点**：
```java
public enum BusinessStatus {
    OPEN(1, "营业中"),
    CLOSED(0, "休息中");
}

// 预约时检查商户状态
public Booking createBooking(...) {
    MerchantProfile merchant = merchantRepository.findById(merchantId);
    if (merchant.getBusinessStatus() == BusinessStatus.CLOSED) {
        throw new BusinessException("商户暂停营业，暂不可预约");
    }
    // ...
}
```

---

### 3.2 预约评价系统

**功能描述**：用户完成预约后可进行评价

**业务价值**：
- 收集用户反馈，提升服务质量
- 评价数据可作为商户排名依据
- 增加用户粘性

**数据模型**：
```sql
CREATE TABLE booking_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    rating TINYINT NOT NULL COMMENT '评分1-5',
    content TEXT COMMENT '评价内容',
    created_at DATETIME NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_merchant_rating (merchant_id, rating)
);
```

**API 设计**：
```
POST /api/reviews          # 提交评价
GET  /api/reviews/merchant/{merchantId}  # 获取商户评价列表
GET  /api/reviews/stats/{merchantId}     # 获取商户评价统计
```

---

### 3.3 预约提醒通知系统

**功能描述**：多渠道预约提醒

**业务价值**：
- 减少用户爽约率
- 提升用户体验

**通知渠道**：
1. **WebSocket 实时推送**：商户端新预约通知
2. **邮件提醒**：预约确认、预约前1小时提醒
3. **短信提醒**：预约前1天提醒（需接入短信服务）

**实现方案**：
```java
@Component
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppointmentWebSocketServer webSocketServer;

    // 发送预约确认邮件
    public void sendBookingConfirmation(Booking booking, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("预约确认通知");
        message.setText(buildBookingContent(booking));
        mailSender.send(message);
    }

    // WebSocket 实时通知
    public void notifyNewBooking(Long merchantId, Booking booking) {
        Map<String, Object> data = Map.of(
            "type", "NEW_BOOKING",
            "bookingId", booking.getId(),
            "userName", booking.getUser().getUsername(),
            "time", booking.getSlot().getStartTime()
        );
        webSocketServer.notifyMerchant(merchantId, toJson(data));
    }
}
```

---

### 3.4 会员等级与积分系统

**功能描述**：用户预约可获得积分，积分可兑换权益

**业务价值**：
- 提高用户粘性和复购率
- VIP 用户可享受优先预约

**数据模型**：
```sql
CREATE TABLE user_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_points INT DEFAULT 0 COMMENT '累计积分',
    current_points INT DEFAULT 0 COMMENT '当前积分',
    level TINYINT DEFAULT 1 COMMENT '会员等级 1-5',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE point_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    points INT NOT NULL COMMENT '积分变动（正为获得，负为消耗）',
    type VARCHAR(20) NOT NULL COMMENT '类型：BOOKING/REDEEM',
    reference_id BIGINT COMMENT '关联ID',
    created_at DATETIME NOT NULL
);
```

**等级规则**：
| 等级 | 所需积分 | 权益 |
|------|----------|------|
| LV1 普通 | 0 | 基础预约 |
| LV2 银卡 | 100 | 优先预约 |
| LV3 金卡 | 500 | 优先预约 + 9折 |
| LV4 铂金 | 1500 | 优先预约 + 85折 |
| LV5 钻石 | 5000 | 优先预约 + 8折 + 专属客服 |

---

### 3.5 预约排队系统

**功能描述**：热门时段满额后，用户可选择排队

**业务价值**：
- 减少用户流失
- 提高预约成功率

**实现方案**：
```sql
CREATE TABLE booking_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    queue_position INT NOT NULL COMMENT '排队位置',
    status VARCHAR(20) DEFAULT 'WAITING' COMMENT 'WAITING/NOTIFIED/EXPIRED',
    created_at DATETIME NOT NULL,
    FOREIGN KEY (slot_id) REFERENCES appointment_slots(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**业务逻辑**：
1. 用户预约满额时段 → 进入排队
2. 有人取消 → 自动通知排队第一人
3. 排队用户 15 分钟内确认 → 预约成功
4. 超时未确认 → 通知下一人

---

### 3.6 付费预约（押金/全款）

**功能描述**：商户可设置预约需要支付押金或全款

**业务价值**：
- 降低爽约率
- 增加商户现金流

**支付方式**：
- 微信支付（需接入微信支付API）
- 支付宝支付

**退款规则**：
- 提前24小时取消：全额退款
- 提前2小时取消：退50%
- 2小时内取消：不退款

---

## 四、前端可视化增强

### 4.1 商户数据看板（ECharts）

**图表类型**：
1. **营业额趋势图**：折线图，展示近7/30天营业额
2. **预约来源分布**：饼图，展示各渠道预约占比
3. **时段热度图**：热力图，展示各时段预约热度
4. **服务排行**：柱状图，展示热门服务TOP10

**实现示例**：
```vue
<template>
  <div class="dashboard">
    <!-- 营业额趋势 -->
    <el-card>
      <v-chart :option="turnoverOption" style="height: 300px" />
    </el-card>

    <!-- 预约分布 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <v-chart :option="sourceOption" style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <v-chart :option="rankOption" style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
```

---

## 五、实施路线图

### 阶段一：核心增强（1-2周）

| 序号 | 功能 | 优先级 | 预估工时 |
|------|------|--------|----------|
| 1 | WebSocket 实时通知 | ⭐⭐⭐⭐⭐ | 2天 |
| 2 | Spring Task 定时任务 | ⭐⭐⭐⭐⭐ | 1天 |
| 3 | AOP 公共字段填充 | ⭐⭐⭐⭐ | 0.5天 |
| 4 | Spring Cache 缓存 | ⭐⭐⭐⭐ | 1天 |

### 阶段二：数据增强（1周）

| 序号 | 功能 | 优先级 | 预估工时 |
|------|------|--------|----------|
| 1 | 营业额统计 | ⭐⭐⭐⭐ | 1天 |
| 2 | 预约转化率统计 | ⭐⭐⭐⭐ | 0.5天 |
| 3 | 热门服务排行 | ⭐⭐⭐ | 0.5天 |
| 4 | Excel 报表导出 | ⭐⭐⭐ | 1天 |
| 5 | ECharts 可视化 | ⭐⭐⭐ | 1天 |

### 阶段三：商业增强（2周）

| 序号 | 功能 | 优先级 | 预估工时 |
|------|------|--------|----------|
| 1 | 商户营业状态 | ⭐⭐⭐ | 0.5天 |
| 2 | 预约评价系统 | ⭐⭐⭐ | 2天 |
| 3 | 预约提醒通知 | ⭐⭐⭐ | 1天 |
| 4 | 会员积分系统 | ⭐⭐ | 3天 |
| 5 | 预约排队系统 | ⭐⭐ | 2天 |
| 6 | 付费预约（可选） | ⭐ | 3天 |

---

## 六、面试亮点总结

通过以上增强，你的项目将具备以下面试亮点：

### 技术层面
1. **WebSocket 实时通信**：理解长连接、心跳机制、集群方案
2. **Spring Task 定时任务**：掌握 Cron 表达式、分布式定时任务演进
3. **AOP 切面编程**：深入理解动态代理、切面原理
4. **Spring Cache 缓存**：理解缓存策略、缓存一致性、缓存穿透/击穿/雪崩
5. **Excel 导出**：掌握 Apache POI、大文件导出优化

### 架构层面
1. **缓存架构设计**：多级缓存、缓存失效策略
2. **实时通信架构**：WebSocket 集群、消息推送
3. **定时任务架构**：单机到分布式的演进

### 业务层面
1. **商业闭环设计**：预约 → 提醒 → 评价 → 复购
2. **用户粘性设计**：会员等级、积分体系
3. **数据驱动运营**：可视化报表、数据导出

---

## 七、参考资源

- sky-take-out 源码：https://github.com/shuhongfan/sky-take-out
- Spring WebSocket 文档：https://docs.spring.io/spring-framework/reference/web/websocket.html
- Spring Cache 文档：https://docs.spring.io/spring-framework/reference/integration/cache.html
- Apache POI 文档：https://poi.apache.org/
- ECharts 文档：https://echarts.apache.org/
