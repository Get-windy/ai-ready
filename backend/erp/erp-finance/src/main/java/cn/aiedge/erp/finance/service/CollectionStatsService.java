package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.CollectionStatsDTO;

import java.time.LocalDate;

/**
 * 回款统计Service
 */
public interface CollectionStatsService {

    /**
     * 回款统计(汇总 + 分组明细)
     *
     * @param startDate 开始日期(收款日期)
     * @param endDate   结束日期(收款日期)
     * @param groupBy   分组维度: day/week/month/staff/customer
     */
    CollectionStatsDTO stats(LocalDate startDate, LocalDate endDate, String groupBy);
}
