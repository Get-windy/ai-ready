package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import cn.aiedge.erp.stock.mapper.StockBomItemMapper;
import cn.aiedge.erp.stock.mapper.StockBomMapper;
import cn.aiedge.erp.stock.service.StockBomService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockBomServiceImpl extends ServiceImpl<StockBomMapper, StockBom> implements StockBomService {

    @Autowired
    private StockBomItemMapper stockBomItemMapper;

    private String generateBomNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "BOM" + dateStr + randomStr;
    }

    @Override
    public Page<StockBom> pageList(String keyword, Long productId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockBom> wrapper = new LambdaQueryWrapper<>();
        // 修复: 只有当keyword不为空时才添加like条件，避免产生空括号SQL语法错误
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(StockBom::getBomNo, keyword)
                    .or()
                    .like(StockBom::getBomName, keyword));
        }
        wrapper.eq(productId != null, StockBom::getProductId, productId)
                .eq(status != null, StockBom::getStatus, status)
                .orderByDesc(StockBom::getCreateTime);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockBom createBom(StockBom bom, List<StockBomItem> items) {
        Long userId = StpUtil.getLoginIdAsLong();
        bom.setTenantId(userId);
        bom.setBomNo(generateBomNo());
        if (bom.getBomType() == null) {
            bom.setBomType(1);
        }
        if (bom.getStatus() == null) {
            bom.setStatus(1);
        }
        bom.setCreateBy(userId);
        bom.setCreateTime(LocalDateTime.now());
        this.save(bom);

        if (items != null && !items.isEmpty()) {
            BigDecimal totalCost = BigDecimal.ZERO;
            for (StockBomItem item : items) {
                item.setBomId(bom.getId());
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ONE);
                }
                if (item.getUnitCost() == null) {
                    item.setUnitCost(BigDecimal.ZERO);
                }
                item.setCost(item.getQuantity().multiply(item.getUnitCost()));
                item.setCreateTime(LocalDateTime.now());
                stockBomItemMapper.insert(item);
                totalCost = totalCost.add(item.getCost());
            }
            bom.setTotalCost(totalCost);
            this.updateById(bom);
        }

        return bom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockBom updateBom(Long id, StockBom bom, List<StockBomItem> items) {
        StockBom existing = this.getById(id);
        if (existing == null) {
            throw BusinessException.notFound("BOM不存在");
        }

        existing.setBomName(bom.getBomName());
        existing.setProductId(bom.getProductId());
        existing.setProductCode(bom.getProductCode());
        existing.setProductName(bom.getProductName());
        existing.setProductSpec(bom.getProductSpec());
        existing.setProductUnit(bom.getProductUnit());
        existing.setOutputQuantity(bom.getOutputQuantity());
        existing.setBomType(bom.getBomType());
        existing.setEffectiveDate(bom.getEffectiveDate());
        existing.setExpireDate(bom.getExpireDate());
        existing.setRemark(bom.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        this.updateById(existing);

        // Delete old items and save new items
        stockBomItemMapper.delete(new LambdaQueryWrapper<StockBomItem>()
                .eq(StockBomItem::getBomId, id));

        if (items != null && !items.isEmpty()) {
            BigDecimal totalCost = BigDecimal.ZERO;
            for (StockBomItem item : items) {
                item.setBomId(id);
                item.setId(null);
                if (item.getQuantity() == null) {
                    item.setQuantity(BigDecimal.ONE);
                }
                if (item.getUnitCost() == null) {
                    item.setUnitCost(BigDecimal.ZERO);
                }
                item.setCost(item.getQuantity().multiply(item.getUnitCost()));
                item.setCreateTime(LocalDateTime.now());
                stockBomItemMapper.insert(item);
                totalCost = totalCost.add(item.getCost());
            }
            existing.setTotalCost(totalCost);
            this.updateById(existing);
        }

        return existing;
    }

    @Override
    public List<StockBomItem> getItems(Long bomId) {
        return stockBomItemMapper.selectList(
                new LambdaQueryWrapper<StockBomItem>()
                        .eq(StockBomItem::getBomId, bomId)
                        .orderByAsc(StockBomItem::getId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockBom enableBom(Long id) {
        StockBom bom = this.getById(id);
        if (bom == null) {
            throw BusinessException.notFound("BOM不存在");
        }
        bom.setStatus(1);
        bom.setUpdateTime(LocalDateTime.now());
        this.updateById(bom);
        return bom;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockBom disableBom(Long id) {
        StockBom bom = this.getById(id);
        if (bom == null) {
            throw BusinessException.notFound("BOM不存在");
        }
        bom.setStatus(0);
        bom.setUpdateTime(LocalDateTime.now());
        this.updateById(bom);
        return bom;
    }
}
