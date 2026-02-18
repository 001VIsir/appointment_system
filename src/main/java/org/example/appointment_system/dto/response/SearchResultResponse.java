package org.example.appointment_system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索结果DTO。
 *
 * <p>包含搜索结果的元数据和结果列表。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索结果")
public class SearchResultResponse {

    @Schema(description = "搜索结果列表")
    private List<SearchResultItem> items;

    @Schema(description = "总记录数")
    private Long totalCount;

    @Schema(description = "当前页码")
    private Integer page;

    @Schema(description = "每页数量")
    private Integer pageSize;

    @Schema(description = "总页数")
    private Integer totalPages;

    /**
     * 搜索结果项DTO。
     *
     * <p>可以是商户或预约任务。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索结果项")
    public static class SearchResultItem {

        @Schema(description = "结果类型: MERCHANT, TASK", example = "MERCHANT")
        private String type;

        @Schema(description = "商户ID（type为MERCHANT时）")
        private Long merchantId;

        @Schema(description = "商户名称", example = "XX洗车店")
        private String merchantName;

        @Schema(description = "商户地址", example = "北京市朝阳区XX路123号")
        private String merchantAddress;

        @Schema(description = "商户描述")
        private String merchantDescription;

        @Schema(description = "商户电话", example = "13800138000")
        private String merchantPhone;

        @Schema(description = "预约任务ID（type为TASK时）")
        private Long taskId;

        @Schema(description = "任务标题", example = "洗车服务")
        private String taskTitle;

        @Schema(description = "任务描述")
        private String taskDescription;

        @Schema(description = "任务日期")
        private LocalDate taskDate;

        @Schema(description = "服务项目名称", example = "普通洗车")
        private String serviceName;

        @Schema(description = "服务类别", example = "VEHICLE")
        private String serviceCategory;

        @Schema(description = "服务价格", example = "50.00")
        private Double servicePrice;

        @Schema(description = "已预约人数", example = "5")
        private Integer bookedCount;

        @Schema(description = "总容量", example = "20")
        private Integer totalCapacity;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
