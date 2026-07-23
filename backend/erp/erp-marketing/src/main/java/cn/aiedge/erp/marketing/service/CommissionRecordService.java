package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.dto.StaffCommissionSummaryDTO;
import cn.aiedge.erp.marketing.entity.CommissionRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CommissionRecordService extends IService<CommissionRecord> {

    /**
     * 员工提成汇总分页
     *
     * @param current   页码
     * @param size      每页数量
     * @param startDate 开始日期 (yyyy-MM-dd, 可空)
     * @param endDate   结束日期 (yyyy-MM-dd, 可空)
     * @param keyword   员工姓名关键词 (可空)
     */
    Page<StaffCommissionSummaryDTO> pageStaffSummary(long current, long size,
                                                     String startDate, String endDate, String keyword);
}
