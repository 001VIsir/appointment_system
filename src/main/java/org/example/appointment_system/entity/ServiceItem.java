package org.example.appointment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.appointment_system.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ServiceItem实体，代表商家提供的服务项目。
 *
 * <p>此实体存储商家可向客户提供的服务信息。每个服务项目属于特定商家，
 * 可与多个预约任务关联。</p>
 *
 * <p>实体包含：</p>
 * <ul>
 *   <li>服务名称和描述</li>
 *   <li>类别分类</li>
 *   <li>时长（分钟）</li>
 *   <li>价格</li>
 *   <li>用于软删除的启用状态</li>
 * </ul>
 */
@Entity
@Table(name = "service_items", indexes = {
    @Index(name = "idx_service_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_service_category", columnList = "category"),
    @Index(name = "idx_service_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
public class ServiceItem {

    /**
     * 服务项目的唯一标识符。
     * 由数据库自动生成。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 拥有此服务项目的商家资料。
     * 与MerchantProfile的多对一关系。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private MerchantProfile merchant;

    /**
     * 服务名称。
     * 必填字段，向客户展示。
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 服务的可选描述。
     * 可提供服务的详细信息。
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 服务类别。
     * 用于分类和筛选。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ServiceCategory category = ServiceCategory.GENERAL;

    /**
     * 服务时长，以分钟为单位。
     * 默认为30分钟。
     */
    @Column(name = "duration", nullable = false)
    private Integer duration = 30;

    /**
     * 服务价格。
     * 使用BigDecimal进行精确的货币计算。
     * 默认为0.00（免费或待定价格）。
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * 服务是否启用且可预约。
     * 支持软删除：未启用的服务会被隐藏但不会被删除。
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * 服务项目创建时的时间戳。
     * 持久化时自动设置。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 服务项目最后更新时间的时间戳。
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
     * 使用必填字段创建服务项目的便捷构造函数。
     *
     * @param merchant   拥有此服务项目的商家资料
     * @param name       服务名称
     * @param category   服务类别
     * @param duration   服务时长（分钟）
     * @param price      服务价格
     */
    public ServiceItem(MerchantProfile merchant, String name, ServiceCategory category,
                       Integer duration, BigDecimal price) {
        this.merchant = merchant;
        this.name = name;
        this.category = category;
        this.duration = duration;
        this.price = price;
    }

    /**
     * 使用所有字段创建服务项目的完整构造函数。
     *
     * @param merchant    拥有此服务项目的商家资料
     * @param name        服务名称
     * @param description 服务描述
     * @param category    服务类别
     * @param duration    服务时长（分钟）
     * @param price       服务价格
     * @param active      服务是否启用
     */
    public ServiceItem(MerchantProfile merchant, String name, String description,
                       ServiceCategory category, Integer duration, BigDecimal price, Boolean active) {
        this.merchant = merchant;
        this.name = name;
        this.description = description;
        this.category = category;
        this.duration = duration;
        this.price = price;
        this.active = active;
    }
}
