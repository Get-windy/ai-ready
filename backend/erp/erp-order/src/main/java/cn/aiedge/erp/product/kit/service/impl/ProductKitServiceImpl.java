package cn.aiedge.erp.product.kit.service.impl;

import cn.aiedge.erp.product.kit.entity.ProductKit;
import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import cn.aiedge.erp.product.kit.mapper.ProductKitItemMapper;
import cn.aiedge.erp.product.kit.mapper.ProductKitMapper;
import cn.aiedge.erp.product.kit.service.ProductKitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductKitServiceImpl extends ServiceImpl<ProductKitMapper, ProductKit> implements ProductKitService {

    private final ProductKitItemMapper kitItemMapper;

    @Override
    public ProductKit getByKitCode(String kitCode) {
        return lambdaQuery()
                .eq(ProductKit::getKitCode, kitCode)
                .eq(ProductKit::getDeleted, 0)
                .one();
    }

    @Override
    public Page<ProductKit> pageList(String keyword, Integer kitType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductKit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductKit::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ProductKit::getKitCode, keyword)
                    .or().like(ProductKit::getKitName, keyword)
                    .or().like(ProductKit::getProductName, keyword));
        }
        if (kitType != null) {
            wrapper.eq(ProductKit::getKitType, kitType);
        }
        if (status != null) {
            wrapper.eq(ProductKit::getStatus, status);
        }
        wrapper.orderByDesc(ProductKit::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<ProductKit> listActiveKits() {
        return baseMapper.selectActiveKits();
    }

    @Override
    public List<ProductKit> listByKitType(Integer kitType) {
        return baseMapper.selectByKitType(kitType);
    }

    @Override
    public String generateKitCode() {
        String prefix = "KIT";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<ProductKit> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(ProductKit::getKitCode, prefix + dateStr)
                .eq(ProductKit::getDeleted, 0)
                .orderByDesc(ProductKit::getKitCode)
                .last("LIMIT 1");
        ProductKit lastKit = getOne(wrapper);
        int seq = 1;
        if (lastKit != null) {
            String lastNo = lastKit.getKitCode();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductKit createKit(ProductKit kit, List<ProductKitItem> items) {
        kit.setKitCode(generateKitCode());
        kit.setStatus(1);
        kit.setActive(true);
        kit.setKitCost(BigDecimal.ZERO);
        kit.setProfitRate(BigDecimal.ZERO);
        save(kit);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                ProductKitItem item = items.get(i);
                item.setKitId(kit.getId());
                item.setLineNo(i + 1);
                item.setTenantId(kit.getTenantId());
                item.setOptional(false);
                item.setSubstitutable(false);
                calculateItemCost(item);
                kitItemMapper.insert(item);
            }
        }
        calculateKitCost(kit.getId());
        return getById(kit.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductKit updateKit(Long kitId, ProductKit kit, List<ProductKitItem> items) {
        ProductKit existing = getById(kitId);
        if (existing == null) {
            throw new RuntimeException("套装不存在");
        }
        kit.setId(kitId);
        updateById(kit);
        if (items != null) {
            List<ProductKitItem> existingItems = getKitItems(kitId);
            for (ProductKitItem oldItem : existingItems) {
                kitItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                ProductKitItem item = items.get(i);
                item.setKitId(kitId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setOptional(false);
                item.setSubstitutable(false);
                calculateItemCost(item);
                kitItemMapper.insert(item);
            }
        }
        calculateKitCost(kitId);
        return getById(kitId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductKit copyKit(Long kitId) {
        ProductKit source = getById(kitId);
        if (source == null) {
            throw new RuntimeException("套装不存在");
        }
        ProductKit copy = new ProductKit();
        copy.setKitName(source.getKitName() + "(副本)");
        copy.setKitType(source.getKitType());
        copy.setKitPrice(source.getKitPrice());
        copy.setAllowSplit(source.getAllowSplit());
        copy.setAllowPartial(source.getAllowPartial());
        copy.setMinQuantity(source.getMinQuantity());
        copy.setMaxQuantity(source.getMaxQuantity());
        copy.setDescription(source.getDescription());
        List<ProductKitItem> sourceItems = getKitItems(kitId);
        return createKit(copy, sourceItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateKit(Long kitId) {
        ProductKit kit = getById(kitId);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        kit.setActive(true);
        updateById(kit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateKit(Long kitId) {
        ProductKit kit = getById(kitId);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        kit.setActive(false);
        updateById(kit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateKitCost(Long kitId) {
        BigDecimal totalCost = kitItemMapper.sumCostByKitId(kitId);
        ProductKit kit = getById(kitId);
        kit.setKitCost(totalCost != null ? totalCost : BigDecimal.ZERO);
        if (kit.getKitPrice() != null && kit.getKitPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitRate = kit.getKitPrice().subtract(kit.getKitCost())
                    .divide(kit.getKitPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            kit.setProfitRate(profitRate);
        }
        updateById(kit);
    }

    @Override
    public List<ProductKitItem> getKitItems(Long kitId) {
        return kitItemMapper.selectByKitId(kitId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductKitItem addKitItem(Long kitId, ProductKitItem item) {
        ProductKit kit = getById(kitId);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        List<ProductKitItem> existingItems = getKitItems(kitId);
        item.setKitId(kitId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(kit.getTenantId());
        item.setOptional(false);
        item.setSubstitutable(false);
        calculateItemCost(item);
        kitItemMapper.insert(item);
        calculateKitCost(kitId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductKitItem updateKitItem(Long itemId, ProductKitItem item) {
        ProductKitItem existing = kitItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("套装组件不存在");
        }
        item.setId(itemId);
        calculateItemCost(item);
        kitItemMapper.updateById(item);
        calculateKitCost(existing.getKitId());
        return kitItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeKitItem(Long itemId) {
        ProductKitItem item = kitItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("套装组件不存在");
        }
        kitItemMapper.deleteById(itemId);
        calculateKitCost(item.getKitId());
    }

    @Override
    public BigDecimal calculateKitPrice(Long kitId) {
        ProductKit kit = getById(kitId);
        return kit != null ? kit.getKitPrice() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateKitProfitRate(Long kitId) {
        ProductKit kit = getById(kitId);
        return kit != null ? kit.getProfitRate() : BigDecimal.ZERO;
    }

    @Override
    public Boolean checkKitAvailability(Long kitId, Long warehouseId, BigDecimal quantity) {
        List<ProductKitItem> items = getKitItems(kitId);
        for (ProductKitItem item : items) {
            BigDecimal requiredQty = item.getQuantity().multiply(quantity);
            log.info("检查库存: 产品ID={}, 仓库ID={}, 需要数量={}", 
                    item.getComponentProductId(), warehouseId, requiredQty);
        }
        return true;
    }

    private void calculateItemCost(ProductKitItem item) {
        BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
        BigDecimal lineCost = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        item.setLineCost(lineCost);
    }
}