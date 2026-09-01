package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.StockBom;
import cn.aiedge.erp.stock.entity.StockBomItem;
import cn.aiedge.erp.stock.mapper.ProductMapper;
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

    @Autowired
    private ProductMapper productMapper;

    private String generateBomNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = IdUtil.randomUUID().substring(0, 6).toUpperCase();
        return "BOM" + dateStr + randomStr;
    }

    @Override
    public Page<StockBom> pageList(String keyword, Long productId, String productName, Integer bomType, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockBom> wrapper = new LambdaQueryWrapper<>();
        // 修复: 只有当keyword不为空时才添加like条件，避免产生空括号SQL语法错误
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(StockBom::getBomNo, keyword)
                    .or()
                    .like(StockBom::getBomName, keyword));
        }
        wrapper.eq(productId != null, StockBom::getProductId, productId)
                .like(productName != null && !productName.trim().isEmpty(), StockBom::getProductName, productName)
                .eq(bomType != null, StockBom::getBomType, bomType)
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
        fillBomSnapshot(bom);
        this.save(bom);

        if (items != null && !items.isEmpty()) {
            BigDecimal totalCost = BigDecimal.ZERO;
            for (StockBomItem item : items) {
                item.setBomId(bom.getId());
                fillItemSnapshot(item);
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
        existing.setTaste(bom.getTaste());
        existing.setModel(bom.getModel());
        existing.setOutputQuantity(bom.getOutputQuantity());
        existing.setBomType(bom.getBomType());
        existing.setEffectiveDate(bom.getEffectiveDate());
        existing.setExpireDate(bom.getExpireDate());
        existing.setRemark(bom.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        // 商品可能已更换，强制从商品主数据回填快照（型号/口味为模板头自有属性，保留用户输入）
        existing.setProductCode(null);
        existing.setProductName(null);
        existing.setProductSpec(null);
        existing.setProductUnit(null);
        existing.setBarcode(null);
        existing.setOrigin(null);
        existing.setBrand(null);
        fillBomSnapshot(existing);
        this.updateById(existing);

        // Delete old items and save new items
        stockBomItemMapper.delete(new LambdaQueryWrapper<StockBomItem>()
                .eq(StockBomItem::getBomId, id));

        if (items != null && !items.isEmpty()) {
            BigDecimal totalCost = BigDecimal.ZERO;
            for (StockBomItem item : items) {
                item.setBomId(id);
                item.setId(null);
                fillItemSnapshot(item);
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

    /**
     * 从商品主数据回填模板头商品快照（成品编码/名称/规格/单位/型号）
     */
    private void fillBomSnapshot(StockBom bom) {
        if (bom.getProductId() == null) {
            return;
        }
        Product p = productMapper.selectById(bom.getProductId());
        if (p == null) {
            return;
        }
        if (bom.getProductCode() == null) bom.setProductCode(p.getProductCode());
        if (bom.getProductName() == null) bom.setProductName(p.getProductName());
        if (bom.getProductSpec() == null) bom.setProductSpec(p.getSpec());
        if (bom.getProductUnit() == null) bom.setProductUnit(p.getUnit());
        if (bom.getModel() == null) bom.setModel(p.getModel());
        if (bom.getBarcode() == null) bom.setBarcode(p.getBarcode());
        if (bom.getOrigin() == null) bom.setOrigin(p.getOrigin());
        if (bom.getBrand() == null) bom.setBrand(p.getBrand());
    }

    /**
     * 从商品主数据回填原料明细商品快照（编码/名称/规格/单位/型号/产地/品牌/条码/图片/成本均价）
     */
    private void fillItemSnapshot(StockBomItem item) {
        if (item.getProductId() == null) {
            return;
        }
        Product p = productMapper.selectById(item.getProductId());
        if (p == null) {
            return;
        }
        if (item.getProductCode() == null) item.setProductCode(p.getProductCode());
        if (item.getProductName() == null) item.setProductName(p.getProductName());
        if (item.getProductSpec() == null) item.setProductSpec(p.getSpec());
        if (item.getProductUnit() == null) item.setProductUnit(p.getUnit());
        if (item.getModel() == null) item.setModel(p.getModel());
        if (item.getOrigin() == null) item.setOrigin(p.getOrigin());
        if (item.getBrand() == null) item.setBrand(p.getBrand());
        if (item.getBarcode() == null) item.setBarcode(p.getBarcode());
        if (item.getImageUrl() == null) item.setImageUrl(p.getImageUrl());
        // 默认成本均价取商品成本价
        if (item.getUnitCost() == null) {
            item.setUnitCost(p.getCostPrice());
        }
    }
}
