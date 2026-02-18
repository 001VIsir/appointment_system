package org.example.appointment_system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索请求DTO。
 *
 * <p>用于用户搜索商户和预约任务的请求参数。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索请求")
public class SearchRequest {

    @Schema(description = "搜索关键词（商户名称、地址或任务标题）", example = "洗车")
    private String keyword;

    @Schema(description = "服务类别", example = "VEHICLE")
    private String category;

    @Schema(description = "排序字段: createdTime, taskDate, bookedCount", example = "createdTime")
    @Builder.Default
    private String sortBy = "createdTime";

    @Schema(description = "排序方向: asc, desc", example = "desc")
    @Builder.Default
    private String sortOrder = "desc";

    @Schema(description = "页码，从1开始", example = "1")
    @Builder.Default
    private Integer page = 1;

    @Schema(description = "每页数量", example = "10")
    @Builder.Default
    private Integer pageSize = 10;
}
