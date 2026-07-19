package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductGradePrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IProductGradePriceService extends IService<ProductGradePrice> {

    /**
     * 按产品ID查询所有等级价格（含所有单位）
     */
    List<ProductGradePrice> getByProductId(Long productId);

    /**
     * 全量覆盖：删除该产品所有等级价格，批量写入新价格
     */
    void batchSave(Long productId, List<ProductGradePrice> priceList);
}
