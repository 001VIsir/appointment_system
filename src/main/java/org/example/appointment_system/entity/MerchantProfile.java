package org.example.appointment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商家资料实体，代表商家的商业资料。
 *
 * <p>此实体为具有MERCHANT角色的用户存储额外的商业信息。每个商家只有一个与其用户账户关联的资料。</p>
 *
 * <p>资料包含：</p>
 * <ul>
 *   <li>商家名称和描述</li>
 *   <li>联系信息（电话、地址）</li>
 *   <li>以JSON格式存储的设置，以提供灵活性</li>
 * </ul>
 */
@Entity
@Table(name = "merchant_profiles", indexes = {
    @Index(name = "idx_merchant_user_id", columnList = "user_id"),
    @Index(name = "idx_merchant_business_name", columnList = "business_name")
})
@Getter
@Setter
@NoArgsConstructor
public class MerchantProfile {

    /**
     * 商家资料的唯一标识符。
     * 由数据库自动生成。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 与此商家资料关联的用户账户。
     * 一对一关系（user_id在数据库中是唯一的）。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 商家的商家名称。
     * 必填字段，向客户展示。
     */
    @Column(name = "business_name", nullable = false, length = 100)
    private String businessName;

    /**
     * 商家的可选描述。
     * 可用于描述提供的服务。
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 联系电话号码。
     * 可选，但建议用于客户联系。
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 商家地址。
     * 可选，对实体位置很有用。
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * 以JSON格式存储的商家设置。
     * 包含会话超时、通知偏好等配置。
     * 示例：{"sessionTimeout": 3600, "notifications": true, "timezone": "Asia/Shanghai"}
     */
    @Column(name = "settings", columnDefinition = "JSON")
    private String settings;

    /**
     * 资料创建时的时间戳。
     * 持久化时自动设置。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 资料最后更新时间的时间戳。
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
     * 创建商家资料的便捷构造函数。
     *
     * @param user         关联的用户账户
     * @param businessName 商家名称
     */
    public MerchantProfile(User user, String businessName) {
        this.user = user;
        this.businessName = businessName;
    }

    /**
     * 使用所有字段创建商家资料的完整构造函数。
     *
     * @param user         关联的用户账户
     * @param businessName 商家名称
     * @param description  商家描述
     * @param phone        联系电话
     * @param address      商家地址
     * @param settings     JSON设置
     */
    public MerchantProfile(User user, String businessName, String description,
                           String phone, String address, String settings) {
        this.user = user;
        this.businessName = businessName;
        this.description = description;
        this.phone = phone;
        this.address = address;
        this.settings = settings;
    }
}
