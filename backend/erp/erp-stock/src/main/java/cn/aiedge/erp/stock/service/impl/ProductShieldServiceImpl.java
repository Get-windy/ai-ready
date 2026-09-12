package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductShield;
import cn.aiedge.erp.stock.mapper.ProductShieldMapper;
import cn.aiedge.erp.stock.service.ProductShieldService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品授权（屏蔽客户）ServiceImpl
 */
@Slf4j
@Service
public class ProductShieldServiceImpl extends ServiceImpl<ProductShieldMapper, ProductShield>
        implements ProductShieldService {

    @Override
    public IPage<ProductShield> getShieldPage(String keyword, String shieldLevel, String region,
                                              Long partnerId, Integer pageNum, Integer pageSize) {
        Page<ProductShield> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        return baseMapper.selectShieldPage(page, keyword, shieldLevel, region, partnerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchShield(List<Long> productIds, List<Long> partnerIds, String partnerNames,
                           String shieldLevel, String region) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        List<Long> partners = (partnerIds == null || partnerIds.isEmpty())
                ? new ArrayList<>(java.util.Collections.singletonList((Long) null))
                : partnerIds;
        List<String> names = partnerNames == null || partnerNames.isEmpty()
                ? new ArrayList<>(java.util.Collections.singletonList((String) null))
                : java.util.Arrays.asList(partnerNames.split(","));

        // 已存在的（商品,客户）组合去重，避免重复插入
        LambdaQueryWrapper<ProductShield> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.in(ProductShield::getProductId, productIds);
        existWrapper.eq(ProductShield::getDeleted, 0);
        Set<String> existKeys = new HashSet<>();
        for (ProductShield s : this.list(existWrapper)) {
            existKeys.add(s.getProductId() + "#" + s.getPartnerId());
        }

        List<ProductShield> toSave = new ArrayList<>();
        for (Long pid : productIds) {
            for (int i = 0; i < partners.size(); i++) {
                Long partnerId = partners.get(i);
                String key = pid + "#" + partnerId;
                if (existKeys.contains(key)) {
                    continue;
                }
                existKeys.add(key);
                ProductShield shield = new ProductShield()
                        .setProductId(pid)
                        .setPartnerId(partnerId)
                        .setPartnerName(i < names.size() ? trimToNull(names.get(i)) : null)
                        .setShieldLevel(shieldLevel == null || shieldLevel.isEmpty() ? "ALL" : shieldLevel)
                        .setRegion(region);
                toSave.add(shield);
            }
        }
        if (toSave.isEmpty()) {
            return 0;
        }
        this.saveBatch(toSave);
        return toSave.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancel(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return this.removeByIds(ids) ? ids.size() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<ProductShield> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ProductShield::getProductId, productIds);
        return this.remove(wrapper) ? productIds.size() : 0;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
