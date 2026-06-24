package cn.aiedge.hr.service;

import cn.aiedge.hr.performance.HrPerformance;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface HrPerformanceService extends IService<HrPerformance> {
    Page<HrPerformance> pageReviews(Page<HrPerformance> page, Long tenantId, Long employeeId, String reviewPeriod);
    Long submitReview(HrPerformance performance);
    void confirmReview(Long id);
}
