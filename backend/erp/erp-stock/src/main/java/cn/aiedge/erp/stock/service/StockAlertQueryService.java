package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 预警查询 Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface StockAlertQueryService {

    /**
     * 分页查询触发上下限预警的商品清单。
     *
     * @param warehouseId    仓库ID（可空，为空查询全部仓库）
     * @param productKeyword 商品名称/货号/条码（可空，模糊）
     * @param brand          品牌（可空，模糊）
     * @param categoryId     商品分类ID（可空，含子分类）
     * @param alertType      预警类型：LOW_STOCK=下限预警，OVER_STOCK=上限预警，空/ALL=全部
     * @param pageNum        页码
     * @param pageSize       每页数量
     */
    IPage<StockAlertQueryVO> page(Long warehouseId, String productKeyword,
                                  String brand, Long categoryId, String alertType,
                                  int pageNum, int pageSize);
}
