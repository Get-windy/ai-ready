package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.CloudProduct;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.mapper.CloudProductMapper;
import cn.aiedge.erp.stock.service.CloudProductService;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云商品库 ServiceImpl：云导入时按云条目生成商品 + 基本单位
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudProductServiceImpl extends ServiceImpl<CloudProductMapper, CloudProduct>
        implements CloudProductService {

    private final ProductService productService;
    private final ProductUnitService productUnitService;

    @Override
    public IPage<CloudProduct> getCloudPage(String keyword, String industryCategory,
                                            Integer pageNum, Integer pageSize) {
        Page<CloudProduct> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        return baseMapper.selectCloudPage(page, keyword, industryCategory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cloudImport(List<Long> cloudIds, Long categoryId) {
        Map<String, Object> result = new HashMap<>();
        if (cloudIds == null || cloudIds.isEmpty()) {
            result.put("imported", 0);
            result.put("skipped", 0);
            return result;
        }
        List<CloudProduct> clouds = this.listByIds(cloudIds);
        int imported = 0;
        int skipped = 0;

        for (CloudProduct cloud : clouds) {
            // 同名校验：同名商品视为已导入，跳过（避免重复建档，商品全局唯一口径）
            LambdaQueryWrapper<Product> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Product::getProductName, cloud.getCloudName());
            dupWrapper.eq(Product::getDeleted, 0);
            if (productService.count(dupWrapper) > 0) {
                skipped++;
                continue;
            }

            Product product = new Product();
            product.setProductName(cloud.getCloudName());
            product.setProductCodeAlias(cloud.getCloudCode());
            product.setProductCode(genProductCode(cloud));
            product.setSpec(cloud.getSpec());
            product.setModel(cloud.getModel());
            product.setOrigin(cloud.getOrigin());
            product.setBrand(cloud.getBrand());
            product.setUnit(cloud.getUnit());
            product.setBarcode(cloud.getBarcode());
            product.setIndustryCategory(cloud.getIndustryCategory() != null ? cloud.getIndustryCategory() : "其他");
            product.setCategoryId(categoryId);
            product.setPurchasePrice(cloud.getPresetPurchasePrice());
            product.setRetailPrice(cloud.getRetailPrice());
            product.setWholesalePrice(cloud.getWholesalePrice());
            product.setShelfLifeDays(cloud.getShelfLifeDays());
            product.setProductType("SINGLE");
            product.setStatus("ENABLED");
            product.setMallShelfStatus(0);
            product.setIsStandardProduct(1);
            productService.save(product);

            // 基本单位行
            ProductUnit baseUnit = new ProductUnit();
            baseUnit.setProductId(product.getId());
            baseUnit.setUnitName(cloud.getUnit() != null ? cloud.getUnit() : "件");
            baseUnit.setIsBaseUnit(1);
            baseUnit.setConversionRate(BigDecimal.ONE);
            baseUnit.setBarcode(cloud.getBarcode());
            baseUnit.setSortOrder(0);
            baseUnit.setPresetPurchasePrice(cloud.getPresetPurchasePrice());
            baseUnit.setWholesalePrice(cloud.getWholesalePrice());
            baseUnit.setRetailPrice(cloud.getRetailPrice());
            productUnitService.save(baseUnit);

            product.setDefaultSalesUnitId(baseUnit.getId());
            product.setDefaultPurchaseUnitId(baseUnit.getId());
            product.setDefaultStockUnitId(baseUnit.getId());
            productService.updateById(product);
            imported++;
        }

        log.info("[云导入] 请求 {} 条，导入 {} 条，跳过 {} 条", cloudIds.size(), imported, skipped);
        result.put("imported", imported);
        result.put("skipped", skipped);
        List<Long> ids = new ArrayList<>();
        result.put("total", ids.size());
        return result;
    }

    private String genProductCode(CloudProduct cloud) {
        if (cloud.getCloudCode() != null && !cloud.getCloudCode().isEmpty()) {
            return cloud.getCloudCode();
        }
        return "SP" + cloud.getId();
    }
}
