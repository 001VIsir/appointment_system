package org.example.appointment_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 时间段响应数据传输对象。
 *
 * <p>包含时段信息，包括可用状态。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {

    /**
     * 时段的唯一标识符。
     */
    private Long id;

    /**
     * 时段的开始时间。
     */
    private LocalTime startTime;

    /**
     * 时段的结束时间。
     */
    private LocalTime endTime;

    /**
     * 时段的最大容量。
     */
    private Integer capacity;

    /**
     * 当前预约数。
     */
    private Integer bookedCount;

    /**
     * 可用名额数量。
     */
    private Integer availableCount;

    /**
     * 时段是否有可用容量。
     */
    private Boolean hasCapacity;
}
