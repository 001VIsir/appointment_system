package org.example.appointment_system.enums;

/**
 * 服务类别枚举，用于对服务项目进行分类。
 *
 * <p>定义商家可以提供的服务类型：</p>
 * <ul>
 *   <li>{@link #GENERAL} - 一般服务</li>
 *   <li>{@link #MEDICAL} - 医疗保健服务</li>
 *   <li>{@link #BEAUTY} - 美容养生服务</li>
 *   <li>{@link #CONSULTATION} - 咨询顾问服务</li>
 *   <li>{@link #EDUCATION} - 教育培训服务</li>
 *   <li>{@link #FITNESS} - 健身运动服务</li>
 *   <li>{@link #OTHER} - 其他未分类服务</li>
 * </ul>
 */
public enum ServiceCategory {

    /**
     * 一般服务类别。
     * 不适合其他类别的服务的默认类别。
     */
    GENERAL("General", "General Services"),

    /**
     * 医疗和保健服务。
     * 包括诊所、医院、牙科服务等。
     */
    MEDICAL("Medical", "Medical & Healthcare"),

    /**
     * 美容和养生服务。
     * 包括沙龙、水疗、皮肤护理等。
     */
    BEAUTY("Beauty", "Beauty & Wellness"),

    /**
     * 咨询和顾问服务。
     * 包括商业咨询、法律建议、财务规划等。
     */
    CONSULTATION("Consultation", "Consulting Services"),

    /**
     * 教育和培训服务。
     * 包括辅导、课程、工作坊等。
     */
    EDUCATION("Education", "Education & Training"),

    /**
     * 健身和运动服务。
     * 包括健身房、私人教练、体育指导等。
     */
    FITNESS("Fitness", "Fitness & Sports"),

    /**
     * 其他未分类服务。
     * 不适合标准类别的服务的备用类别。
     */
    OTHER("Other", "Other Services");

    private final String code;
    private final String displayName;

    ServiceCategory(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 获取类别的短代码。
     *
     * @return 类别代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取人类可读的显示名称。
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
