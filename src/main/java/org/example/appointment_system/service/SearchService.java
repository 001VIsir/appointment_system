package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.SearchRequest;
import org.example.appointment_system.dto.response.SearchResultResponse;
import org.example.appointment_system.dto.response.SearchResultResponse.SearchResultItem;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索服务。
 *
 * <p>提供商户和预约任务的搜索功能，支持分页和排序。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final MerchantProfileRepository merchantRepository;
    private final AppointmentTaskRepository taskRepository;
    private final AppointmentSlotRepository slotRepository;

    /**
     * 搜索商户和预约任务。
     *
     * <p>根据关键词搜索商户名称/地址，或预约任务标题。
     * 返回分页的搜索结果。</p>
     *
     * @param request 搜索请求参数
     * @return 搜索结果
     */
    public SearchResultResponse search(SearchRequest request) {
        log.info("Searching with keyword: {}, category: {}, sortBy: {}, page: {}",
                request.getKeyword(), request.getCategory(), request.getSortBy(), request.getPage());

        // 计算分页参数
        int page = Math.max(1, request.getPage()) - 1; // PageRequest 使用0-based
        int pageSize = Math.min(Math.max(1, request.getPageSize()), 50); // 限制最大50条

        // 创建排序
        Sort sort = createSort(request.getSortBy(), request.getSortOrder());
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        // 分别搜索商户和任务
        List<SearchResultItem> allItems = new ArrayList<>();

        // 搜索商户
        Page<MerchantProfile> merchantPage = merchantRepository.searchMerchants(
                request.getKeyword() != null ? request.getKeyword() : "", pageable);

        for (MerchantProfile merchant : merchantPage.getContent()) {
            allItems.add(convertToSearchResult(merchant));
        }

        // 搜索预约任务（仅公开的激活状态任务）
        Page<AppointmentTask> taskPage = taskRepository.searchTasks(
                request.getKeyword(),
                request.getCategory(),
                pageable);

        for (AppointmentTask task : taskPage.getContent()) {
            allItems.add(convertToSearchResult(task));
        }

        // 合并结果并排序
        allItems = sortResults(allItems, request.getSortBy(), request.getSortOrder());

        // 计算分页信息
        long totalCount = merchantPage.getTotalElements() + taskPage.getTotalElements();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        return SearchResultResponse.builder()
                .items(allItems)
                .totalCount(totalCount)
                .page(request.getPage())
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    /**
     * 创建排序对象。
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return Spring Sort对象
     */
    private Sort createSort(String sortBy, String sortOrder) {
        String field;
        switch (sortBy != null ? sortBy.toLowerCase() : "createdtime") {
            case "taskdate":
                field = "taskDate";
                break;
            case "bookedcount":
                field = "bookedCount";
                break;
            case "createdtime":
            default:
                field = "createdAt";
                break;
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }

    /**
     * 对搜索结果进行排序。
     *
     * @param items     搜索结果列表
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 排序后的结果
     */
    private List<SearchResultItem> sortResults(List<SearchResultItem> items,
                                               String sortBy, String sortOrder) {
        // 由于商户和任务来自不同的表，这里在应用层简单排序
        // 实际生产环境可以使用数据库 UNION 或专门的搜索服务如 Elasticsearch
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);

        return items.stream()
                .sorted((a, b) -> {
                    int result;
                    switch (sortBy != null ? sortBy.toLowerCase() : "createdtime") {
                        case "taskdate":
                            if (a.getTaskDate() == null || b.getTaskDate() == null) {
                                result = 0;
                            } else {
                                result = a.getTaskDate().compareTo(b.getTaskDate());
                            }
                            break;
                        case "bookedcount":
                            Integer countA = a.getBookedCount() != null ? a.getBookedCount() : 0;
                            Integer countB = b.getBookedCount() != null ? b.getBookedCount() : 0;
                            result = countA.compareTo(countB);
                            break;
                        case "createdtime":
                        default:
                            if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                                result = 0;
                            } else {
                                result = a.getCreatedAt().compareTo(b.getCreatedAt());
                            }
                            break;
                    }
                    return ascending ? result : -result;
                })
                .toList();
    }

    /**
     * 将商户实体转换为搜索结果项。
     *
     * @param merchant 商户实体
     * @return 搜索结果项
     */
    private SearchResultItem convertToSearchResult(MerchantProfile merchant) {
        return SearchResultItem.builder()
                .type("MERCHANT")
                .merchantId(merchant.getId())
                .merchantName(merchant.getBusinessName())
                .merchantAddress(merchant.getAddress())
                .merchantDescription(merchant.getDescription())
                .merchantPhone(merchant.getPhone())
                .createdAt(merchant.getCreatedAt())
                .build();
    }

    /**
     * 将预约任务实体转换为搜索结果项。
     *
     * @param task 预约任务实体
     * @return 搜索结果项
     */
    private SearchResultItem convertToSearchResult(AppointmentTask task) {
        ServiceItem service = task.getService();
        MerchantProfile merchant = service != null ? service.getMerchant() : null;

        // 获取已预约人数
        int bookedCount = slotRepository.sumBookedCountByTaskId(task.getId());

        return SearchResultItem.builder()
                .type("TASK")
                .merchantId(merchant != null ? merchant.getId() : null)
                .merchantName(merchant != null ? merchant.getBusinessName() : null)
                .merchantAddress(merchant != null ? merchant.getAddress() : null)
                .taskId(task.getId())
                .taskTitle(task.getTitle())
                .taskDescription(task.getDescription())
                .taskDate(task.getTaskDate())
                .serviceName(service != null ? service.getName() : null)
                .serviceCategory(service != null ? service.getCategory().name() : null)
                .servicePrice(service != null && service.getPrice() != null ? service.getPrice().doubleValue() : null)
                .bookedCount(bookedCount)
                .totalCapacity(task.getTotalCapacity())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
