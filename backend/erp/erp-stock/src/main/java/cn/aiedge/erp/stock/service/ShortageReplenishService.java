package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.ShortageReplenishVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 缺货补货查询 Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ShortageReplenishService {

    /**
     * 分页查询缺货补货（按商品×仓库聚合）
     */
    IPage<ShortageReplenishVO> page(Integer orderStatus, String startDate, String endDate,
                                    Long customerId, Long salesmanId, Long warehouseId,
                                    Integer orderSource, String productKeyword, String supplierName,
                                    Long categoryId, Integer shortageMode, Boolean onlyShortage,
                                    int pageNum, int pageSize);
}
