package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.ProductBarcodeVO;
import cn.aiedge.erp.stock.entity.ProductBarcode;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.mapper.ProductBarcodeMapper;
import cn.aiedge.erp.stock.mapper.ProductBarcodeQueryMapper;
import cn.aiedge.erp.stock.service.ProductBarcodeService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ProductBarcodeServiceImpl extends ServiceImpl<ProductBarcodeMapper, ProductBarcode>
        implements ProductBarcodeService {

    private final ProductBarcodeQueryMapper productBarcodeQueryMapper;
    private final ProductUnitService productUnitService;

    @Override
    public List<ProductBarcode> getByProductId(Long productId) {
        return lambdaQuery()
                .eq(ProductBarcode::getProductId, productId)
                .eq(ProductBarcode::getDeleted, 0)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ProductBarcodeVO> pageBarcodes(Long categoryId, String keyword, String barcodeFilter,
                                                Integer shelfStatus, String status,
                                                String createTimeOp, String createTimeStart,
                                                String purchaseDateOp, String purchaseDateStart,
                                                String sortField, String sortOrder,
                                                int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        IPage<ProductBarcodeVO> page = productBarcodeQueryMapper.selectBarcodePage(new Page<>(pageNum, pageSize), tenantId,
                categoryId, keyword, barcodeFilter, shelfStatus, status,
                createTimeOp, createTimeStart, purchaseDateOp, purchaseDateStart,
                productBarcodeQueryMapper.orderBodyOf(sortField, sortOrder));
        fillDerivedFields(page.getRecords());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductBarcodeVO> listBarcodes(Long categoryId, String keyword, String barcodeFilter,
                                               Integer shelfStatus, String status,
                                               String createTimeOp, String createTimeStart,
                                               String purchaseDateOp, String purchaseDateStart,
                                               String sortField, String sortOrder) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        List<ProductBarcodeVO> rows = productBarcodeQueryMapper.selectBarcodeList(tenantId,
                categoryId, keyword, barcodeFilter, shelfStatus, status,
                createTimeOp, createTimeStart, purchaseDateOp, purchaseDateStart,
                productBarcodeQueryMapper.orderBodyOf(sortField, sortOrder));
        fillDerivedFields(rows);
        return rows;
    }

    /** 条码归一化：空白视为未设置（null），其余去除首尾空格 */
    private String normalizeBarcode(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isEmpty() ? null : value;
    }

    /** 补齐派生展示字段（换算关系 / 上架状态文案），保证页面与导出同一口径 */
    private void fillDerivedFields(List<ProductBarcodeVO> rows) {
        if (rows == null) {
            return;
        }
        for (ProductBarcodeVO vo : rows) {
            vo.setConversionRelation(buildConversionRelation(vo));
            vo.setShelfStatusText(vo.getShelfStatus() != null && vo.getShelfStatus() == 1 ? "已上架" : "未上架");
        }
    }

    private String buildConversionRelation(ProductBarcodeVO vo) {
        if (vo.getConversionRate() == null || vo.getUnitName() == null
                || (vo.getIsBaseUnit() != null && vo.getIsBaseUnit() == 1)) {
            return null;
        }
        String baseUnitName = vo.getBaseUnitName();
        if (baseUnitName == null || baseUnitName.isEmpty()) {
            return null;
        }
        BigDecimal rate = vo.getConversionRate().stripTrailingZeros();
        return "1" + vo.getUnitName() + "=" + rate.toPlainString() + baseUnitName;
    }

    @Override
    public void validateUnitBarcodes(Long productId, List<ProductUnit> units) {
        if (units == null || units.isEmpty()) {
            return;
        }
        // 1) 归一化 + 同一请求内条码查重
        Set<String> codes = new LinkedHashSet<>();
        for (ProductUnit unit : units) {
            String value = normalizeBarcode(unit.getBarcode());
            unit.setBarcode(value);
            if (value != null && !codes.add(value)) {
                throw new RuntimeException("条码 " + value + " 在同一商品内重复");
            }
        }
        if (codes.isEmpty()) {
            return;
        }
        // 2) 跨商品唯一性：单位行与条码表双口径（排除本商品自身）
        long dupUnit = productUnitService.lambdaQuery()
                .in(ProductUnit::getBarcode, codes)
                .eq(ProductUnit::getDeleted, 0)
                .ne(productId != null, ProductUnit::getProductId, productId)
                .count();
        if (dupUnit > 0) {
            throw new RuntimeException("条码已被其它商品单位使用，请检查是否重复");
        }
        long dupBarcode = lambdaQuery()
                .in(ProductBarcode::getBarcode, codes)
                .eq(ProductBarcode::getIsDefault, 1)
                .eq(ProductBarcode::getDeleted, 0)
                .ne(productId != null, ProductBarcode::getProductId, productId)
                .count();
        if (dupBarcode > 0) {
            throw new RuntimeException("条码已被其它商品使用，请检查是否重复");
        }
    }

    @Override
    public void syncUnitBarcodes(Long productId, List<ProductUnit> units) {
        if (productId == null || units == null) {
            return;
        }
        // 重建本商品默认条码记录（与单位表「先删后插」同口径，避免残留陈旧条码）
        lambdaUpdate()
                .eq(ProductBarcode::getProductId, productId)
                .eq(ProductBarcode::getIsDefault, 1)
                .remove();
        for (ProductUnit unit : units) {
            String value = normalizeBarcode(unit.getBarcode());
            unit.setBarcode(value);
            if (value == null || unit.getId() == null) {
                continue;
            }
            save(new ProductBarcode()
                    .setProductId(productId)
                    .setUnitId(unit.getId())
                    .setBarcode(value)
                    .setBarcodeType("OTHER")
                    .setIsDefault(1)
                    .setDeleted(0));
        }
    }

    @Override
    public void updateUnitBarcode(Long unitId, String barcode, String barcodeType) {
        // 只取维护条码所需列：erp_product_unit 在部分环境缺少价格等级列，避免全列查询
        ProductUnit unit = productUnitService.lambdaQuery()
                .select(ProductUnit::getId, ProductUnit::getProductId, ProductUnit::getBarcode)
                .eq(ProductUnit::getId, unitId)
                .one();
        if (unit == null) {
            throw new RuntimeException("商品单位不存在或已删除");
        }
        String value = barcode == null ? null : barcode.trim();
        if (!StringUtils.hasText(value)) {
            value = null;
        }
        if (value != null) {
            // 条码全局唯一：单位行条码不可与其它单位行重复
            long dupUnit = productUnitService.lambdaQuery()
                    .eq(ProductUnit::getBarcode, value)
                    .eq(ProductUnit::getDeleted, 0)
                    .ne(ProductUnit::getId, unitId)
                    .count();
            if (dupUnit > 0) {
                throw new RuntimeException("条码 " + value + " 已被其它商品单位使用");
            }
            // 条码全局唯一：不可与条码表既有条码重复（同一商品同单位除外）
            long dupBarcode = lambdaQuery()
                    .eq(ProductBarcode::getBarcode, value)
                    .eq(ProductBarcode::getDeleted, 0)
                    .and(w -> w.ne(ProductBarcode::getProductId, unit.getProductId())
                            .or().ne(ProductBarcode::getUnitId, unitId))
                    .count();
            if (dupBarcode > 0) {
                throw new RuntimeException("条码 " + value + " 已被其它商品使用");
            }
        }
        unit.setBarcode(value);
        productUnitService.updateById(unit);

        // 同步条码表默认记录（erp_product_barcode 为条码的全局唯一口径，含条码类型）
        ProductBarcode exist = lambdaQuery()
                .eq(ProductBarcode::getProductId, unit.getProductId())
                .eq(ProductBarcode::getUnitId, unitId)
                .eq(ProductBarcode::getIsDefault, 1)
                .eq(ProductBarcode::getDeleted, 0)
                .one();
        if (exist != null) {
            exist.setBarcode(value);
            if (StringUtils.hasText(barcodeType)) {
                exist.setBarcodeType(barcodeType);
            }
            updateById(exist);
        } else if (value != null) {
            save(new ProductBarcode()
                    .setProductId(unit.getProductId())
                    .setUnitId(unitId)
                    .setBarcode(value)
                    .setBarcodeType(StringUtils.hasText(barcodeType) ? barcodeType : "OTHER")
                    .setIsDefault(1)
                    .setDeleted(0));
        }
    }
}
