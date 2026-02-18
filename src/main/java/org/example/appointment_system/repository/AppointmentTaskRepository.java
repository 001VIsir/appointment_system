package org.example.appointment_system.repository;

import org.example.appointment_system.entity.AppointmentTask;
import org.example.appointment_system.entity.ServiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 预约任务实体Repository接口。
 *
 * <p>提供预约任务数据访问操作，包括：</p>
 * <ul>
 *   <li>标准CRUD操作（继承自JpaRepository）</li>
 *   <li>按服务项目查询</li>
 *   <li>按日期和启用状态查询</li>
 *   <li>按服务和日期检查是否存在</li>
 * </ul>
 */
@Repository
public interface AppointmentTaskRepository extends JpaRepository<AppointmentTask, Long> {

    /**
     * 查询指定服务项目的所有预约任务。
     *
     * @param service 要查询的服务项目
     * @return 该服务的预约任务列表
     */
    List<AppointmentTask> findByService(ServiceItem service);

    /**
     * 按服务项目ID查询所有预约任务。
     *
     * @param serviceId 要查询的服务项目ID
     * @return 该服务的预约任务列表
     */
    List<AppointmentTask> findByServiceId(Long serviceId);

    /**
     * 查询指定服务项目的所有启用状态的预约任务。
     *
     * @param service 要查询的服务项目
     * @return 该服务的启用状态预约任务列表
     */
    List<AppointmentTask> findByServiceAndActiveTrue(ServiceItem service);

    /**
     * 按服务项目ID查询所有启用状态的预约任务。
     *
     * @param serviceId 要查询的服务项目ID
     * @return 该服务的启用状态预约任务列表
     */
    List<AppointmentTask> findByServiceIdAndActiveTrue(Long serviceId);

    /**
     * 按ID和服务项目查询预约任务。
     *
     * @param id      预约任务ID
     * @param service 服务项目
     * @return 包含预约任务的Optional（如果找到且属于该服务）
     */
    Optional<AppointmentTask> findByIdAndService(Long id, ServiceItem service);

    /**
     * 按ID和服务项目ID查询预约任务。
     *
     * @param id        预约任务ID
     * @param serviceId 服务项目ID
     * @return 包含预约任务的Optional（如果找到）
     */
    Optional<AppointmentTask> findByIdAndServiceId(Long id, Long serviceId);

    /**
     * 按日期查询所有预约任务。
     *
     * @param taskDate 要查询的日期
     * @return 指定日期的预约任务列表
     */
    List<AppointmentTask> findByTaskDate(LocalDate taskDate);

    /**
     * 按日期查询所有启用状态的预约任务。
     *
     * @param taskDate 要查询的日期
     * @return 指定日期的启用状态预约任务列表
     */
    List<AppointmentTask> findByTaskDateAndActiveTrue(LocalDate taskDate);

    /**
     * 查询指定服务在指定日期的所有预约任务。
     *
     * @param service  服务项目
     * @param taskDate 要查询的日期
     * @return 匹配条件的预约任务列表
     */
    List<AppointmentTask> findByServiceAndTaskDate(ServiceItem service, LocalDate taskDate);

    /**
     * 按服务项目ID和日期查询所有预约任务。
     *
     * @param serviceId 服务项目ID
     * @param taskDate  要查询的日期
     * @return 匹配条件的预约任务列表
     */
    List<AppointmentTask> findByServiceIdAndTaskDate(Long serviceId, LocalDate taskDate);

    /**
     * 查询两个日期之间的所有预约任务（包含两端）。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期范围内的预约任务列表
     */
    List<AppointmentTask> findByTaskDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 查询两个日期之间所有启用状态的预约任务（包含两端）。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期范围内的启用状态预约任务列表
     */
    List<AppointmentTask> findByTaskDateBetweenAndActiveTrue(LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定服务在两个日期之间所有启用状态的预约任务。
     *
     * @param service   服务项目
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日期范围内的启用状态预约任务列表
     */
    List<AppointmentTask> findByServiceAndTaskDateBetweenAndActiveTrue(
            ServiceItem service, LocalDate startDate, LocalDate endDate);

    /**
     * 检查指定服务在指定日期是否存在预约任务。
     *
     * @param service  服务项目
     * @param taskDate 要检查的日期
     * @return 如果该服务在该日期存在任务返回true
     */
    boolean existsByServiceAndTaskDate(ServiceItem service, LocalDate taskDate);

    /**
     * 按服务项目ID和日期检查预约任务是否存在。
     *
     * @param serviceId 服务项目ID
     * @param taskDate  要检查的日期
     * @return 如果该服务在该日期存在任务返回true
     */
    boolean existsByServiceIdAndTaskDate(Long serviceId, LocalDate taskDate);

    /**
     * 统计指定服务的所有预约任务数量。
     *
     * @param service 服务项目
     * @return 预约任务数量
     */
    long countByService(ServiceItem service);

    /**
     * 统计指定服务的启用状态预约任务数量。
     *
     * @param service 服务项目
     * @return 启用状态预约任务数量
     */
    long countByServiceAndActiveTrue(ServiceItem service);

    /**
     * 查询指定服务在指定日期及之后的启用状态预约任务。
     *
     * @param service      服务项目
     * @param taskDateFrom 最小日期（包含）
     * @return 启用状态预约任务列表
     */
    List<AppointmentTask> findByServiceAndTaskDateGreaterThanEqualAndActiveTrue(
            ServiceItem service, LocalDate taskDateFrom);

    /**
     * 按服务项目ID查询指定日期及之后的启用状态预约任务。
     *
     * @param serviceId    服务项目ID
     * @param taskDateFrom 最小日期（包含）
     * @return 启用状态预约任务列表
     */
    List<AppointmentTask> findByServiceIdAndTaskDateGreaterThanEqualAndActiveTrue(
            Long serviceId, LocalDate taskDateFrom);

    /**
     * 查询指定服务的所有禁用状态预约任务。
     *
     * @param service 服务项目
     * @return 禁用状态预约任务列表
     */
    List<AppointmentTask> findByServiceAndActiveFalse(ServiceItem service);

    /**
     * 按ID查询所有公开的启用状态任务（用于签名链接访问）。
     *
     * @param id     预约任务ID
     * @param active 启用状态
     * @return 包含任务的Optional（如果找到且启用）
     */
    Optional<AppointmentTask> findByIdAndActive(Long id, Boolean active);

    /**
     * 分页搜索公开的预约任务。
     *
     * <p>根据关键词搜索任务标题，匹配服务类别，且只返回启用状态的任务。</p>
     *
     * @param keyword 搜索关键词（任务标题）
     * @param category 服务类别
     * @param pageable 分页参数
     * @return 预约任务分页结果
     */
    @Query("SELECT t FROM AppointmentTask t JOIN t.service s WHERE t.active = true AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR s.category = :category)")
    Page<AppointmentTask> searchTasks(
            @Param("keyword") String keyword,
            @Param("category") String category,
            Pageable pageable);

    /**
     * 统计搜索结果数量。
     *
     * @param keyword 搜索关键词
     * @param category 服务类别
     * @return 匹配的任务数量
     */
    long countByActiveTrueAndTitleContainingIgnoreCaseOrServiceCategory(
            String keyword, String category);
}
