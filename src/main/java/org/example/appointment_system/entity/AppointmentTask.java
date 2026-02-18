package org.example.appointment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AppointmentTask实体，代表可预约的预约任务。
 *
 * <p>此实体代表商家为其服务创建的特定预约机会。每个任务关联一个服务项目，
 * 并具有特定的日期。任务包含用户可以预约的时间段。</p>
 *
 * <p>实体包含：</p>
 * <ul>
 *   <li>关联的服务项目</li>
 *   <li>任务标题和描述</li>
 *   <li>任务日期</li>
 *   <li>整个任务的总容量</li>
 *   <li>用于可用性控制的启用状态</li>
 * </ul>
 */
@Entity
@Table(name = "appointment_tasks", indexes = {
    @Index(name = "idx_task_service_id", columnList = "service_id"),
    @Index(name = "idx_task_date", columnList = "task_date"),
    @Index(name = "idx_task_active", columnList = "active"),
    @Index(name = "idx_task_service_date", columnList = "service_id, task_date")
})
@Getter
@Setter
@NoArgsConstructor
public class AppointmentTask {

    /**
     * 预约任务的唯一标识符。
     * 由数据库自动生成。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 此预约任务关联的服务项目。
     * 与ServiceItem的多对一关系。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    /**
     * 预约任务的标题。
     * 必填字段，向客户展示。
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 预约任务的可选描述。
     * 可提供此任务的额外详细信息。
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 预约任务的日期。
     * 用于调度和筛选。
     */
    @Column(name = "task_date", nullable = false)
    private LocalDate taskDate;

    /**
     * 此任务的最大预约总数。
     * 这是总体容量限制。
     * 默认为1。
     */
    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity = 1;

    /**
     * 预约任务是否启用且可预约。
     * 支持软删除：未启用的任务会被隐藏但不会被删除。
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * 预约任务创建时的时间戳。
     * 持久化时自动设置。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 预约任务最后更新时间的时间戳。
     * 修改时自动更新。
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 预持久化回调，用于设置时间戳。
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 预更新回调，用于更新修改时间戳。
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 使用必填字段创建预约任务的便捷构造函数。
     *
     * @param service       此任务所属的服务项目
     * @param title         任务标题
     * @param taskDate      任务日期
     * @param totalCapacity 最大容量
     */
    public AppointmentTask(ServiceItem service, String title, LocalDate taskDate, Integer totalCapacity) {
        this.service = service;
        this.title = title;
        this.taskDate = taskDate;
        this.totalCapacity = totalCapacity;
    }

    /**
     * 使用所有字段创建预约任务的完整构造函数。
     *
     * @param service       此任务所属的服务项目
     * @param title         任务标题
     * @param description   任务描述
     * @param taskDate      任务日期
     * @param totalCapacity 最大容量
     * @param active        任务是否启用
     */
    public AppointmentTask(ServiceItem service, String title, String description,
                           LocalDate taskDate, Integer totalCapacity, Boolean active) {
        this.service = service;
        this.title = title;
        this.description = description;
        this.taskDate = taskDate;
        this.totalCapacity = totalCapacity;
        this.active = active;
    }

    /**
     * 检查此任务是否启用。
     * 便于阅读的便捷方法。
     *
     * @return 如果任务启用则返回true
     */
    @Transient
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }
}
