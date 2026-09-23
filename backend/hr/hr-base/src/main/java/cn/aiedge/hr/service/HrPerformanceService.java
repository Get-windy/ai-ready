package cn.aiedge.hr.service;

import cn.aiedge.hr.performance.HrPerformance;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 绩效考核服务
 */
public interface HrPerformanceService extends IService<HrPerformance> {

    Page<HrPerformance> pageReviews(Page<HrPerformance> page, Long tenantId,
                                    Long employeeId, String reviewPeriod,
                                    String reviewType, String level, Integer status, Long deptId);

    Long submitReview(HrPerformance performance);

    void updateReview(HrPerformance performance);

    void confirmReview(Long id);

    void deleteReview(Long id);

    /** 绩效统计：考核单数 / 平均分 / 各等级人数 */
    Map<String, Object> statistics(String reviewPeriod, Long deptId);
}
