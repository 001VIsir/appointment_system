package org.example.appointment_system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.SearchRequest;
import org.example.appointment_system.dto.response.SearchResultResponse;
import org.example.appointment_system.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索操作的REST控制器。
 *
 * <p>提供公开的商户和预约任务搜索接口。</p>
 *
 * <h3>接口：</h3>
 * <ul>
 *   <li>GET /api/search - 搜索商户和预约任务</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Public search APIs for merchants and appointment tasks")
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索商户和预约任务。
     *
     * <p>公开接口，无需认证。根据关键词搜索商户名称/地址，
     * 或预约任务标题。支持按创建时间、预约日期、预约人数排序。</p>
     *
     * @param keyword 搜索关键词
     * @param category 服务类别筛选
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param page 页码
     * @param pageSize 每页数量
     * @return 搜索结果
     */
    @GetMapping
    @Operation(
        summary = "Search merchants and tasks",
        description = "Searches for merchants and appointment tasks by keyword. " +
                      "Returns paginated results sorted by specified field."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(schema = @Schema(implementation = SearchResultResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters"
        )
    })
    public ResponseEntity<SearchResultResponse> search(
            @Parameter(description = "Search keyword (merchant name/address or task title)")
            @ModelAttribute SearchRequest request) {
        log.info("Search request: keyword={}, category={}, sortBy={}, sortOrder={}, page={}, pageSize={}",
                request.getKeyword(), request.getCategory(), request.getSortBy(),
                request.getSortOrder(), request.getPage(), request.getPageSize());

        SearchResultResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }
}
