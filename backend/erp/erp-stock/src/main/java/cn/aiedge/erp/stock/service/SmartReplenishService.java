package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.SmartReplenishVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 智能补货查询 Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SmartReplenishService {

    /**
     * 分页查询智能补货（按商品聚合，每商品一行）
     *
     * @param startDate      销售日期区间开始（yyyy-MM-dd，可空）
     * @param endDate        销售日期区间结束（yyyy-MM-dd，可空）
     * @param stockDays      备货天数
     * @param warehouseId    仓库ID（可空，为空统计全部仓库）
     * @param productKeyword 商品名称/编码/货号/条码（可空）
     * @param supplierName   供货商（可空，模糊）
     * @param categoryId     商品分类ID（可空）
     * @param minPlanQty     计划采购数量下限（可空）
     */
    IPage<SmartReplenishVO> page(String startDate, String endDate,
                                 Integer stockDays, Long warehouseId,
                                 String productKeyword, String supplierName,
                                 Long categoryId, BigDecimal minPlanQty,
                                 int pageNum, int pageSize);
}
