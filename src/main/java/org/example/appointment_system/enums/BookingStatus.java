package org.example.appointment_system.enums;

/**
 * 预约状态枚举，用于跟踪预约生命周期。
 *
 * <p>定义预约的可能状态：</p>
 * <ul>
 *   <li>{@link #PENDING} - 预约已创建但尚未确认</li>
 *   <li>{@link #CONFIRMED} - 预约已确认并准备就绪</li>
 *   <li>{@link #CANCELLED} - 预约已被用户或系统取消</li>
 *   <li>{@link #COMPLETED} - 预约已完成（预约已结束）</li>
 * </ul>
 */
public enum BookingStatus {

    /**
     * 待确认状态 - 预约已创建但尚未确认。
     * 这是用户创建预约时的初始状态。
     */
    PENDING("pending", "Pending"),

    /**
     * 已确认状态 - 预约已确认并准备就绪。
     * 时间槽已保留，用户应按预约时间出席。
     */
    CONFIRMED("confirmed", "Confirmed"),

    /**
     * 已取消状态 - 预约已被取消。
     * 可以由用户或系统取消（例如：爽约）。
     */
    CANCELLED("cancelled", "Cancelled"),

    /**
     * 已完成状态 - 预约已完成。
     * 这是预约结束后的最终状态。
     */
    COMPLETED("completed", "Completed");

    private final String code;
    private final String displayName;

    BookingStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 获取此状态的数据库代码。
     *
     * @return 状态代码
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

    /**
     * 检查此预约是否可以取消。
     * 只有PENDING和CONFIRMED状态的预约可以取消。
     *
     * @return 如果可以取消则返回true
     */
    public boolean canCancel() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * 检查此预约是否可以确认。
     * 只有PENDING状态的预约可以确认。
     *
     * @return 如果可以确认则返回true
     */
    public boolean canConfirm() {
        return this == PENDING;
    }

    /**
     * 检查此预约是否可以标记为已完成。
     * 只有CONFIRMED状态的预约可以完成。
     *
     * @return 如果可以完成则返回true
     */
    public boolean canComplete() {
        return this == CONFIRMED;
    }

    /**
     * 检查此预约是否处于活动状态。
     * 活动的预约是PENDING或CONFIRMED状态。
     *
     * @return 如果预约处于活动状态则返回true
     */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * 检查此预约是否处于最终状态。
     * 最终状态是CANCELLED或COMPLETED。
     *
     * @return 如果预约处于最终状态则返回true
     */
    public boolean isFinal() {
        return this == CANCELLED || this == COMPLETED;
    }
}
