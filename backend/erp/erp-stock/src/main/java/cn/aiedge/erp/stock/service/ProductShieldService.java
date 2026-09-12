package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductShield;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品授权（屏蔽客户）Service
 */
public interface ProductShieldService extends IService<ProductShield> {

    IPage<ProductShield> getShieldPage(String keyword, String shieldLevel, String region,
                                       Long partnerId, Integer pageNum, Integer pageSize);

    /**
     * 批量屏蔽：为多个商品 × 多个客户建立屏蔽关系（已存在则跳过）
     */
    int batchShield(List<Long> productIds, List<Long> partnerIds, String partnerNames,
                    String shieldLevel, String region);

    /**
     * 批量取消：按授权记录ID取消
     */
    int batchCancel(List<Long> ids);

    /**
     * 批量取消：按商品ID取消全部授权
     */
    int cancelByProductIds(List<Long> productIds);
}
