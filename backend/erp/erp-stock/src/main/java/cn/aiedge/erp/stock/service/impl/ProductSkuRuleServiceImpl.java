package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductCategory;
import cn.aiedge.erp.stock.entity.ProductSkuRule;
import cn.aiedge.erp.stock.mapper.ProductCategoryMapper;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.ProductSkuRuleMapper;
import cn.aiedge.erp.stock.service.ProductSkuRuleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ProductSkuRuleServiceImpl extends ServiceImpl<ProductSkuRuleMapper, ProductSkuRule>
        implements ProductSkuRuleService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;

    @Override
    public String generateSku(Long ruleId, Long productId) {
        ProductSkuRule rule = getById(ruleId);
        if (rule == null) return null;

        Product product = productMapper.selectById(productId);
        if (product == null) return null;

        String format = rule.getRuleFormat();
        String separator = rule.getSeparator() != null ? rule.getSeparator() : "-";

        // 替换模板变量
        String sku = format;
        sku = sku.replace("{CATEGORY}", getCategoryCode(product.getCategoryId()));
        sku = sku.replace("{PRODUCT_CODE}", product.getProductCode() != null ? product.getProductCode() : "");
        sku = sku.replace("{SEQ}", generateSeq(rule));

        return sku;
    }

    @Override
    public boolean setDefault(Long id) {
        // 清除其他默认
        lambdaUpdate().eq(ProductSkuRule::getIsDefault, 1).set(ProductSkuRule::getIsDefault, 0).update();
        // 设置当前为默认
        return lambdaUpdate().eq(ProductSkuRule::getId, id).set(ProductSkuRule::getIsDefault, 1).update();
    }

    private String getCategoryCode(Long categoryId) {
        if (categoryId == null) return "";
        ProductCategory cat = categoryMapper.selectById(categoryId);
        return cat != null ? cat.getCategoryCode() : "";
    }

    private synchronized String generateSeq(ProductSkuRule rule) {
        // 简单实现: 查询当前最大序号+1
        String seqStr = new DecimalFormat(buildFormat(rule.getSeqLength()))
                .format(rule.getSeqStart());
        // 更新seq_start
        lambdaUpdate()
                .eq(ProductSkuRule::getId, rule.getId())
                .set(ProductSkuRule::getSeqStart, rule.getSeqStart() + 1)
                .update();
        return seqStr;
    }

    private String buildFormat(int length) {
        return "0".repeat(Math.max(1, length));
    }
}
