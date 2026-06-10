package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductSkuRule;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ProductSkuRuleService extends IService<ProductSkuRule> {
    String generateSku(Long ruleId, Long productId);
    boolean setDefault(Long id);
}
