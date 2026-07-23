package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.CustomerActiveAnalysisDTO;
import cn.aiedge.erp.sale.dto.PromotionAnalysisDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 销售分析服务
 */
public interface SaleAnalysisService {

    /**
     * 客户活跃分析分页
     *
     * @param current    页码
     * @param size       每页数量
     * @param days       近N天窗口 (默认30)
     * @param activeDays 活跃阈值天数 (默认30: 30天内有单=活跃)
     * @param silentDays 沉默阈值天数 (默认90: 90天内有单=一般, 否则=沉默)
     * @param keyword    客户名称关键词
     */
    Page<CustomerActiveAnalysisDTO> pageCustomerActive(long current, long size,
                                                       Integer days, Integer activeDays, Integer silentDays,
                                                       String keyword);

    /**
     * 促销效果分析
     *
     * @param startDate 开始日期 (yyyy-MM-dd, 可空)
     * @param endDate   结束日期 (yyyy-MM-dd, 可空)
     */
    PromotionAnalysisDTO promotionAnalysis(String startDate, String endDate);
}
