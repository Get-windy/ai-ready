package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.StockAlertReplenishVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 库存预警补货查询Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface StockAlertReplenishService {

    /**
     * 库存预警补货分页查询
     *
     * @param warehouseId  仓库ID（空=全部）
     * @param keyword      商品名称/编码/货号关键字
     * @param brand        品牌
     * @param supplierName 所属供应商（模糊）
     * @param remark       备注
     * @param onlyLowStock 是否只显示下限预警商品
     * @param categoryId   商品分类ID（空=全部）
     * @param pageNum      页码
     * @param pageSize     每页大小
     */
    IPage<StockAlertReplenishVO> page(Long warehouseId, String keyword, String brand, String supplierName,
                                      String remark, Boolean onlyLowStock, Long categoryId,
                                      int pageNum, int pageSize);
}
