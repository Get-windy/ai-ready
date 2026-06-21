package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductRecommend;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 推荐商品Service接口
 */
public interface ProductRecommendService extends IService<ProductRecommend> {

    /**
     * 获取商品的推荐列表
     */
    List<ProductRecommend> getByProductId(Long productId);

    /**
     * 批量保存推荐商品(先删后插)
     */
    boolean batchSave(Long productId, List<ProductRecommend> recommends);
}
