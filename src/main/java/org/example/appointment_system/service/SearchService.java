package org.example.appointment_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointment_system.dto.request.SearchRequest;
import org.example.appointment_system.dto.response.SearchResultResponse;
import org.example.appointment_system.dto.response.SearchResultResponse.SearchResultItem;
import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.MerchantProfile;
import org.example.appointment_system.entity.ServiceItem;
import org.example.appointment_system.enums.ServiceCategory;
import org.example.appointment_system.repository.AppointmentSlotRepository;
import org.example.appointment_system.repository.AppointmentTaskRepository;
import org.example.appointment_system.repository.MerchantProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

        // 创建分页（不使用数据库排序，因为后续会在应用层进行内存排序）
        Pageable pageable = PageRequest.of(page, pageSize);

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
                pageable);

        // 在 Service 层过滤 category
        List<AppointmentTask> filteredTasks = taskPage.getContent();
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            try {
                ServiceCategory category = ServiceCategory.valueOf(request.getCategory());
                filteredTasks = taskPage.getContent().stream()
                        .filter(task -> task.getService() != null && task.getService().getCategory() == category)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category: {}", request.getCategory());
            }
        }

        for (AppointmentTask task : filteredTasks) {
            allItems.add(convertToSearchResult(task));
        }

        // 合并结果并排序
        allItems = sortResults(allItems, request.getSortBy(), request.getSortOrder());

        // 计算分页信息（只计算过滤后的任务数量）
        long totalCount = merchantPage.getTotalElements() + filteredTasks.size();
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

        // 分离商户和任务
        List<SearchResultItem> merchants = items.stream()
                .filter(item -> "MERCHANT".equals(item.getType()))
                .toList();

        List<SearchResultItem> tasks = items.stream()
                .filter(item -> "TASK".equals(item.getType()))
                .toList();

        // 对任务进行排序
        List<SearchResultItem> sortedTasks = sortTasks(tasks, sortBy, sortOrder);

        // 合并结果：先任务后商户，或根据排序方向调整
        List<SearchResultItem> result = new ArrayList<>();
        result.addAll(sortedTasks);
        result.addAll(merchants);

        return result;
    }

    /**
     * 对任务列表进行排序。
     */
    private List<SearchResultItem> sortTasks(List<SearchResultItem> tasks,
                                            String sortBy, String sortOrder) {
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);
        String sortField = sortBy != null ? sortBy.toLowerCase() : "createdtime";

        Comparator<SearchResultItem> comparator;

        switch (sortField) {
            case "taskdate":
                comparator = Comparator.comparing(
                        item -> item.getTaskDate() != null ? item.getTaskDate() : java.time.LocalDate.MAX,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "bookedcount":
                comparator = Comparator.comparing(
                        item -> item.getBookedCount() != null ? item.getBookedCount() : 0);
                break;
            case "createdtime":
            default:
                comparator = Comparator.comparing(
                        item -> item.getCreatedAt() != null ? item.getCreatedAt() : java.time.LocalDateTime.MIN,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return tasks.stream()
                .sorted(comparator)
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
