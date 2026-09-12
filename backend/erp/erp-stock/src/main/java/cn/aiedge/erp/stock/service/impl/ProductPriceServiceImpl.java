package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.BatchModifyPriceDTO;
import cn.aiedge.erp.stock.dto.GradePriceVO;
import cn.aiedge.erp.stock.dto.ProductPriceVO;
import cn.aiedge.erp.stock.entity.CustomerGradeDiscount;
import cn.aiedge.erp.stock.entity.CustomerGradePrice;
import cn.aiedge.erp.stock.entity.CustomerProductPrice;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.mapper.CustomerGradeDiscountMapper;
import cn.aiedge.erp.stock.mapper.CustomerGradePriceMapper;
import cn.aiedge.erp.stock.mapper.CustomerProductPriceMapper;
import cn.aiedge.erp.stock.mapper.GradePriceQueryMapper;
import cn.aiedge.erp.stock.mapper.ProductPriceQueryMapper;
import cn.aiedge.erp.stock.mapper.ProductUnitMapper;
import cn.aiedge.erp.stock.entity.ProductGrade;
import cn.aiedge.erp.stock.service.ProductGradeService;
import cn.aiedge.erp.stock.service.ProductPriceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 商品价格管理服务实现
 * <p>
 * 价格单一口径：商品价格始终读/写 {@code erp_product_unit} 的价格列，本服务不另建价格副本。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPriceServiceImpl extends ServiceImpl<CustomerGradeDiscountMapper, CustomerGradeDiscount>
        implements ProductPriceService {

    private final ProductPriceQueryMapper productPriceQueryMapper;
    private final GradePriceQueryMapper gradePriceQueryMapper;
    private final ProductUnitMapper productUnitMapper;
    private final CustomerGradePriceMapper customerGradePriceMapper;
    private final CustomerProductPriceMapper customerProductPriceMapper;
    private final ProductGradeService productGradeService;

    // ══════════════════════ 子标签 1：商品价格批量修改 ══════════════════════

    @Override
    @Transactional(readOnly = true)
    public IPage<ProductPriceVO> pagePrices(Long categoryId, String keyword, String brand, String unitType,
                                            Long productId, Integer shelfStatus,
                                            String purchaseDateOp, String purchaseDate,
                                            String stockQtyOp, String stockQty, Boolean showHierarchy,
                                            int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        IPage<ProductPriceVO> page = productPriceQueryMapper.selectPricePage(new Page<>(pageNum, pageSize), tenantId,
                categoryId, keyword, brand, unitType, productId, shelfStatus,
                purchaseDateOp, purchaseDate, stockQtyOp, stockQty, showHierarchy);
        fillDerivedFields(page.getRecords());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductPriceVO> listPrices(Long categoryId, String keyword, String brand, String unitType,
                                           Long productId, Integer shelfStatus,
                                           String purchaseDateOp, String purchaseDate,
                                           String stockQtyOp, String stockQty, Boolean showHierarchy) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        List<ProductPriceVO> rows = productPriceQueryMapper.selectPriceList(tenantId, categoryId, keyword, brand,
                unitType, productId, shelfStatus, purchaseDateOp, purchaseDate, stockQtyOp, stockQty, showHierarchy);
        fillDerivedFields(rows);
        return rows;
    }

    /** 换算关系 / 上架状态文案：与《商品条码》同一口径 */
    private void fillDerivedFields(List<ProductPriceVO> rows) {
        if (rows == null) {
            return;
        }
        for (ProductPriceVO vo : rows) {
            vo.setShelfStatusText(vo.getShelfStatus() != null && vo.getShelfStatus() == 1 ? "已上架" : "未上架");
            vo.setConversionRelation(buildConversionRelation(vo));
        }
    }

    private String buildConversionRelation(ProductPriceVO vo) {
        if (vo.getConversionRate() == null || vo.getUnitName() == null
                || (vo.getIsBaseUnit() != null && vo.getIsBaseUnit() == 1)) {
            return null;
        }
        String baseUnitName = vo.getBaseUnitName();
        if (baseUnitName == null || baseUnitName.isEmpty()) {
            return null;
        }
        return "1" + vo.getUnitName() + "=" + vo.getConversionRate().stripTrailingZeros().toPlainString() + baseUnitName;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchModifyPrices(BatchModifyPriceDTO dto) {
        if (dto == null || dto.getUnitIds() == null || dto.getUnitIds().isEmpty()) {
            throw new IllegalArgumentException("请先勾选需要修改价格的商品行");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("请至少填写一个价格修改项");
        }
        List<ProductUnit> units = productUnitMapper.selectBatchIds(dto.getUnitIds());
        if (units == null || units.isEmpty()) {
            throw new IllegalArgumentException("未找到选中的商品单位行，请刷新后重试");
        }
        // 入参先整体校验：缺值/负数直接拒绝，避免"改了 0 条"这种静默失败
        for (BatchModifyPriceDTO.PriceModifyItem item : dto.getItems()) {
            if (!StringUtils.hasText(item.getField())) {
                continue;
            }
            boolean ruleMode = "RULE".equalsIgnoreCase(item.getMode());
            if (ruleMode) {
                if (item.getCalcValue() == null) {
                    throw new IllegalArgumentException("按规则改价需填写计算数");
                }
                if (!StringUtils.hasText(item.getBasePriceField())) {
                    throw new IllegalArgumentException("按规则改价需选择基础价");
                }
            } else if (item.getValue() == null) {
                throw new IllegalArgumentException("直接改价需填写新价格");
            }
            if (item.getValue() != null && item.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("价格不能为负数");
            }
            if (item.getCalcValue() != null && item.getCalcValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("计算数不能为负数");
            }
        }
        int updated = 0;
        for (ProductUnit unit : units) {
            boolean changed = false;
            for (BatchModifyPriceDTO.PriceModifyItem item : dto.getItems()) {
                if (!StringUtils.hasText(item.getField())) {
                    continue;
                }
                BigDecimal target = resolveTargetValue(unit, item);
                if (target == null) {
                    continue;
                }
                applyPriceField(unit, item.getField(), target);
                changed = true;
            }
            if (changed) {
                productUnitMapper.updateById(unit);
                updated++;
            }
        }
        return updated;
    }

    /** 计算目标价格：FIXED=直接改价；RULE=基础价 ×|+|-|÷ 计算数 */
    private BigDecimal resolveTargetValue(ProductUnit unit, BatchModifyPriceDTO.PriceModifyItem item) {
        String mode = StringUtils.hasText(item.getMode()) ? item.getMode() : "FIXED";
        if ("RULE".equalsIgnoreCase(mode)) {
            BigDecimal base = readPriceField(unit, item.getBasePriceField());
            BigDecimal calcValue = item.getCalcValue();
            if (calcValue == null) {
                throw new IllegalArgumentException("按规则改价需填写计算数");
            }
            if (base == null) {
                // 基础价为空时不能静默跳过，否则前端会看到"已修改 0 条"却不知原因
                throw new IllegalArgumentException("商品「" + unit.getUnitName() + "」的所选基础价为空，无法按规则计算");
            }
            String op = StringUtils.hasText(item.getCalcOperator()) ? item.getCalcOperator() : "*";
            BigDecimal result = switch (op) {
                case "+" -> base.add(calcValue);
                case "-" -> base.subtract(calcValue);
                case "/" -> calcValue.compareTo(BigDecimal.ZERO) == 0
                        ? null : base.divide(calcValue, 4, RoundingMode.HALF_UP);
                default -> base.multiply(calcValue);
            };
            if (result != null && result.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("按规则计算后的价格为负数，请检查基础价与计算数");
            }
            return result == null ? null : result.setScale(2, RoundingMode.HALF_UP);
        }
        return item.getValue() == null ? null : item.getValue().setScale(2, RoundingMode.HALF_UP);
    }

    /** 价格字段读写（字段名 = 前端列 key，与 erp_product_unit 价格列一一对应） */
    private BigDecimal readPriceField(ProductUnit unit, String field) {
        if (!StringUtils.hasText(field)) {
            return null;
        }
        return switch (field) {
            case "presetPurchasePrice" -> unit.getPresetPurchasePrice();
            case "referenceCost" -> unit.getReferenceCost();
            case "recentPurchasePrice" -> unit.getRecentPurchasePrice();
            case "wholesalePrice" -> unit.getWholesalePrice();
            case "retailPrice" -> unit.getRetailPrice();
            case "minSalePrice" -> unit.getMinSalePrice();
            case "minDiscount" -> unit.getMinDiscount();
            case "gradePrice1" -> unit.getGradePrice1();
            case "gradePrice2" -> unit.getGradePrice2();
            case "gradePrice3" -> unit.getGradePrice3();
            case "gradePrice4" -> unit.getGradePrice4();
            case "gradePrice5" -> unit.getGradePrice5();
            case "gradePrice6" -> unit.getGradePrice6();
            case "gradePrice7" -> unit.getGradePrice7();
            case "gradePrice8" -> unit.getGradePrice8();
            default -> null;
        };
    }

    private void applyPriceField(ProductUnit unit, String field, BigDecimal value) {
        switch (field) {
            case "presetPurchasePrice" -> unit.setPresetPurchasePrice(value);
            case "referenceCost" -> unit.setReferenceCost(value);
            case "recentPurchasePrice" -> unit.setRecentPurchasePrice(value);
            case "wholesalePrice" -> unit.setWholesalePrice(value);
            case "retailPrice" -> unit.setRetailPrice(value);
            case "minSalePrice" -> unit.setMinSalePrice(value);
            case "minDiscount" -> unit.setMinDiscount(value);
            case "gradePrice1" -> unit.setGradePrice1(value);
            case "gradePrice2" -> unit.setGradePrice2(value);
            case "gradePrice3" -> unit.setGradePrice3(value);
            case "gradePrice4" -> unit.setGradePrice4(value);
            case "gradePrice5" -> unit.setGradePrice5(value);
            case "gradePrice6" -> unit.setGradePrice6(value);
            case "gradePrice7" -> unit.setGradePrice7(value);
            case "gradePrice8" -> unit.setGradePrice8(value);
            default -> { /* 非价格列忽略 */ }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listBrands() {
        return productPriceQueryMapper.selectBrands(MyBatisPlusConfig.getCurrentTenantIdValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listCustomerGrades() {
        return productPriceQueryMapper.selectCustomerGrades(MyBatisPlusConfig.getCurrentTenantIdValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listGradeNames() {
        // 按槽位号（gradeLevel）排序：index+1 即 grade_price_1~8 的槽位，与列表列顺序/导出表头严格一致
        return productGradeService.getActiveGrades().stream()
                .filter(g -> g.getGradeName() != null)
                .sorted(java.util.Comparator.comparing(g -> g.getGradeLevel() == null ? 99 : g.getGradeLevel()))
                .map(ProductGrade::getGradeName)
                .toList();
    }

    // ══════════════════════ 统一取价（供销售/采购/零售复用） ══════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> resolvePrice(Long customerId, Long productId, Long unitId) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        ProductUnit unit = unitId != null ? productUnitMapper.selectById(unitId) : baseUnitOf(productId);
        if (unit == null) {
            throw new IllegalArgumentException("未找到商品单位价格行，请先在商品档案维护单位");
        }
        String gradeName = customerId == null ? null : gradePriceQueryMapper.selectCustomerLevel(tenantId, customerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unitId", unit.getId());
        result.put("gradeName", gradeName);

        // 1) 客户指定价（优先级最高）
        CustomerProductPrice customerPrice = customerId == null ? null
                : gradePriceQueryMapper.selectCustomerPriceRule(tenantId, customerId, productId);
        if (customerPrice != null) {
            BigDecimal price = rulePrice(unit, customerPrice.getBasePriceType(),
                    customerPrice.getCalcOperator(), customerPrice.getCalcValue(), customerPrice.getPrice());
            result.put("price", price);
            result.put("source", "CUSTOMER");
            result.put("sourceText", "客户指定价");
            result.put("priceRule", customerPrice.getPriceRule());
            result.put("ruleId", customerPrice.getId());
            return result;
        }

        // 2) 客户级别指定价
        CustomerGradePrice gradePrice = gradeName == null ? null
                : gradePriceQueryMapper.selectGradePriceRule(tenantId, gradeName, productId);
        if (gradePrice != null) {
            BigDecimal price = rulePrice(unit, gradePrice.getBasePriceType(),
                    gradePrice.getCalcOperator(), gradePrice.getCalcValue(), null);
            result.put("price", price);
            result.put("source", "GRADE_PRICE");
            result.put("sourceText", "级别指定价");
            result.put("priceRule", gradePrice.getPriceRule());
            result.put("ruleId", gradePrice.getId());
            return result;
        }

        // 3) 客户级别折扣（级别默认价）
        CustomerGradeDiscount discount = gradeName == null ? null
                : gradePriceQueryMapper.selectGradeDiscountRule(tenantId, gradeName);
        if (discount != null) {
            BigDecimal price = rulePrice(unit, discount.getBasePriceType(),
                    discount.getCalcOperator(), discount.getCalcValue(), null);
            result.put("price", price);
            result.put("source", "GRADE_DISCOUNT");
            result.put("sourceText", "客户级别折扣");
            result.put("priceRule", discount.getPreviewText());
            result.put("ruleId", discount.getId());
            return result;
        }

        // 4) 商品单位价兜底（零售价 → 批发价 → 成本）
        BigDecimal fallback = firstNotNull(unit.getRetailPrice(), unit.getWholesalePrice(),
                unit.getReferenceCost(), BigDecimal.ZERO);
        result.put("price", fallback);
        result.put("source", "PRODUCT");
        result.put("sourceText", "商品零售价");
        return result;
    }

    private ProductUnit baseUnitOf(Long productId) {
        if (productId == null) {
            return null;
        }
        return productUnitMapper.selectOne(new LambdaQueryWrapper<ProductUnit>()
                .eq(ProductUnit::getProductId, productId)
                .orderByDesc(ProductUnit::getIsBaseUnit)
                .orderByAsc(ProductUnit::getSortOrder)
                .last("LIMIT 1"));
    }

    /** 规则价：basePriceType+运算 优先；无基础价时用固定值（客户指定价的「指定价 X」） */
    private BigDecimal rulePrice(ProductUnit unit, String basePriceType, String operator,
                                 BigDecimal calcValue, BigDecimal fixedPrice) {
        String fieldKey = resolveBaseFieldKey(basePriceType);
        if (fieldKey == null) {
            BigDecimal price = fixedPrice != null ? fixedPrice : calcValue;
            return price == null ? BigDecimal.ZERO : price.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal base = readPriceField(unit, fieldKey);
        if (base == null) {
            base = BigDecimal.ZERO;
        }
        BigDecimal value = calcValue == null ? BigDecimal.ZERO : calcValue;
        String op = StringUtils.hasText(operator) ? operator : "*";
        BigDecimal result = switch (op) {
            case "+" -> base.add(value);
            case "-" -> base.subtract(value);
            case "/" -> value.compareTo(BigDecimal.ZERO) == 0 ? base : base.divide(value, 4, RoundingMode.HALF_UP);
            default -> base.multiply(value);
        };
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    /** 基础价类型（中文）→ erp_product_unit 价格字段 key；等级名按 sort_order 映射到 gradePriceN */
    private String resolveBaseFieldKey(String basePriceType) {
        if (!StringUtils.hasText(basePriceType)) {
            return null;
        }
        String type = basePriceType.trim();
        switch (type) {
            case "零售价":
                return "retailPrice";
            case "批发价":
                return "wholesalePrice";
            case "最低售价":
                return "minSalePrice";
            case "最低折扣":
            case "最低折扣(%)":
                return "minDiscount";
            case "预设进价":
                return "presetPurchasePrice";
            case "参考成本":
                return "referenceCost";
            default:
                break;
        }
        List<ProductGrade> grades = productGradeService.getActiveGrades();
        for (int i = 0; i < grades.size(); i++) {
            if (type.equals(grades.get(i).getGradeName())) {
                Integer level = grades.get(i).getGradeLevel();
                return "gradePrice" + (level != null ? level : i + 1);
            }
        }
        return null;
    }

    private BigDecimal firstNotNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    // ══════════════════════ 子标签 2：客户级别折扣设置 ══════════════════════

    @Override
    @Transactional(readOnly = true)
    public IPage<CustomerGradeDiscount> pageGradeDiscounts(String keyword, int pageNum, int pageSize) {
        IPage<CustomerGradeDiscount> page = baseMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CustomerGradeDiscount>()
                        .like(StringUtils.hasText(keyword), CustomerGradeDiscount::getGradeName, keyword)
                        .orderByDesc(CustomerGradeDiscount::getUpdateTime));
        fillModifierNames(page.getRecords());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerGradeDiscount> listGradeDiscounts(String keyword) {
        List<CustomerGradeDiscount> rows = baseMapper.selectList(new LambdaQueryWrapper<CustomerGradeDiscount>()
                .like(StringUtils.hasText(keyword), CustomerGradeDiscount::getGradeName, keyword)
                .orderByDesc(CustomerGradeDiscount::getUpdateTime));
        fillModifierNames(rows);
        return rows;
    }

    /** 最后修改人：按 update_by 批量回填姓名（沿用系统「最后修改人」口径） */
    private void fillModifierNames(List<CustomerGradeDiscount> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream().map(CustomerGradeDiscount::getUpdateBy)
                .filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (Map<String, Object> row : baseMapper.selectModifierNames(userIds)) {
            Object id = row.get("id");
            Object name = row.get("name");
            if (id != null) {
                names.put(((Number) id).longValue(), name == null ? null : String.valueOf(name));
            }
        }
        rows.forEach(r -> r.setLastModifierName(names.get(r.getUpdateBy())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGradeDiscount(CustomerGradeDiscount entity) {
        if (!StringUtils.hasText(entity.getGradeName())) {
            throw new IllegalArgumentException("请选择客户级别");
        }
        if (!StringUtils.hasText(entity.getBasePriceType())) {
            throw new IllegalArgumentException("请选择基础价");
        }
        entity.setCalcOperator(StringUtils.hasText(entity.getCalcOperator()) ? entity.getCalcOperator() : "*");
        if (entity.getCalcValue() == null) {
            entity.setCalcValue(BigDecimal.ONE);
        }
        if (entity.getCalcValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("计算数不能为负数");
        }
        long dup = baseMapper.selectCount(new LambdaQueryWrapper<CustomerGradeDiscount>()
                .eq(CustomerGradeDiscount::getGradeName, entity.getGradeName())
                .ne(entity.getId() != null, CustomerGradeDiscount::getId, entity.getId()));
        if (dup > 0) {
            throw new IllegalArgumentException("客户级别「" + entity.getGradeName() + "」已设置级别默认价，请直接修改");
        }
        entity.setPreviewText(buildGradePreview(entity));
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getId() == null) {
            entity.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
            baseMapper.insert(entity);
        } else {
            baseMapper.updateById(entity);
        }
    }

    private String buildGradePreview(CustomerGradeDiscount entity) {
        String value = entity.getCalcValue() == null ? "1"
                : entity.getCalcValue().stripTrailingZeros().toPlainString();
        return "客户级别【" + entity.getGradeName() + "】的订货价格=" + entity.getBasePriceType()
                + entity.getCalcOperator() + value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGradeDiscount(Long id) {
        baseMapper.deleteById(id);
    }

    // ══════════════════════ 子标签 3：级别指定价设置 ══════════════════════

    @Override
    @Transactional(readOnly = true)
    public IPage<GradePriceVO> pageLevelPrices(String gradeName, String keyword, String brand,
                                               int pageNum, int pageSize) {
        return gradePriceQueryMapper.selectLevelPricePage(new Page<>(pageNum, pageSize),
                MyBatisPlusConfig.getCurrentTenantIdValue(), gradeName, keyword, brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradePriceVO> listLevelPrices(String gradeName, String keyword, String brand) {
        return gradePriceQueryMapper.selectLevelPriceList(MyBatisPlusConfig.getCurrentTenantIdValue(),
                gradeName, keyword, brand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLevelPrice(CustomerGradePrice entity) {
        validateRuleInput(entity.getGradeName(), entity.getProductId(), entity.getCategoryId(), entity.getCalcValue());
        entity.setPriceRule(buildPriceRule(entity.getBasePriceType(), entity.getCalcOperator(), entity.getCalcValue()));
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getId() == null) {
            entity.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
            customerGradePriceMapper.insert(entity);
        } else {
            customerGradePriceMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLevelPrices(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请先勾选需要删除的数据");
        }
        customerGradePriceMapper.deleteByIds(ids);
    }

    // ══════════════════════ 子标签 4：客户指定价设置 ══════════════════════

    @Override
    @Transactional(readOnly = true)
    public IPage<GradePriceVO> pageCustomerPrices(Long customerId, Long productId, String keyword, String brand,
                                                  int pageNum, int pageSize) {
        return gradePriceQueryMapper.selectCustomerPricePage(new Page<>(pageNum, pageSize),
                MyBatisPlusConfig.getCurrentTenantIdValue(), customerId, productId, keyword, brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradePriceVO> listCustomerPrices(Long customerId, Long productId, String keyword, String brand) {
        return gradePriceQueryMapper.selectCustomerPriceList(MyBatisPlusConfig.getCurrentTenantIdValue(),
                customerId, productId, keyword, brand);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCustomerPrice(CustomerProductPrice entity) {
        if (entity.getCustomerId() == null) {
            throw new IllegalArgumentException("请选择客户");
        }
        validateRuleInput(null, entity.getProductId(), entity.getCategoryId(), entity.getCalcValue());
        entity.setPriceRule(buildPriceRule(entity.getBasePriceType(), entity.getCalcOperator(), entity.getCalcValue()));
        if (entity.getPriceType() == null) {
            entity.setPriceType(StringUtils.hasText(entity.getBasePriceType()) ? "GRADE" : "SPECIFIED");
        }
        if (!"GRADE".equals(entity.getPriceType())) {
            entity.setPrice(entity.getCalcValue());
        }
        if (entity.getIsActive() == null) {
            entity.setIsActive(1);
        }
        if (entity.getId() == null) {
            entity.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
            customerProductPriceMapper.insert(entity);
        } else {
            customerProductPriceMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerPrices(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请先勾选需要删除的数据");
        }
        customerProductPriceMapper.deleteByIds(ids);
    }

    private void validateRuleInput(String gradeName, Long productId, Long categoryId, BigDecimal calcValue) {
        if (calcValue == null) {
            throw new IllegalArgumentException("请填写计算数");
        }
        if (calcValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("计算数不能为负数");
        }
        if (productId == null && categoryId == null) {
            throw new IllegalArgumentException("请选择商品或商品分类");
        }
    }

    /** 生成价格规则展示文本：有基础价 → 「基础价+计算符+计算数」，否则「指定价 X」 */
    private String buildPriceRule(String basePriceType, String calcOperator, BigDecimal calcValue) {
        String value = calcValue == null ? "0" : calcValue.stripTrailingZeros().toPlainString();
        if (!StringUtils.hasText(basePriceType)) {
            return "指定价 " + value;
        }
        String op = StringUtils.hasText(calcOperator) ? calcOperator : "+";
        return basePriceType + op + value;
    }

    // ══════════════════════ 导入 ══════════════════════

    @Override
    public Map<String, Object> importRules(String type, List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        if (rows == null || rows.isEmpty()) {
            result.put("total", 0);
            result.put("success", 0);
            result.put("failure", 0);
            result.put("errors", List.of("导入文件没有可导入的数据行"));
            return result;
        }
        boolean isCustomer = "customer".equalsIgnoreCase(type);
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int lineNo = i + 2;
            total++;
            try {
                String productName = str(row.get("productName"));
                Long productId = firstNonNull(toLong(row.get("productId")),
                        productName == null ? null : gradePriceQueryMapper.selectProductIdByName(tenantId, productName));
                if (productId == null) {
                    throw new IllegalArgumentException("商品「" + productName + "」在商品档案中不存在，请先建档");
                }
                String gradeName = str(row.get("gradeName"));
                String basePriceType = str(row.get("basePriceType"));
                String calcOperator = str(row.get("calcOperator"));
                BigDecimal calcValue = toDecimal(row.get("calcValue"));
                if (isCustomer) {
                    String customerName = str(row.get("customerName"));
                    Long customerId = firstNonNull(toLong(row.get("customerId")),
                            customerName == null ? null : gradePriceQueryMapper.selectCustomerIdByName(tenantId, customerName));
                    if (customerId == null) {
                        throw new IllegalArgumentException("客户「" + customerName + "」在往来单位中不存在，请先建档");
                    }
                    CustomerProductPrice entity = new CustomerProductPrice();
                    entity.setCustomerId(customerId);
                    entity.setProductId(productId);
                    entity.setProductName(productName);
                    entity.setProductCode(str(row.get("productCode")));
                    entity.setUnitName(str(row.get("unitName")));
                    entity.setBasePriceType(basePriceType);
                    entity.setCalcOperator(calcOperator);
                    entity.setCalcValue(calcValue);
                    saveCustomerPrice(entity);
                } else {
                    CustomerGradePrice entity = new CustomerGradePrice();
                    entity.setGradeName(gradeName);
                    entity.setProductId(productId);
                    entity.setProductName(productName);
                    entity.setProductCode(str(row.get("productCode")));
                    entity.setUnitName(str(row.get("unitName")));
                    entity.setBasePriceType(basePriceType);
                    entity.setCalcOperator(calcOperator);
                    entity.setCalcValue(calcValue);
                    saveLevelPrice(entity);
                }
                success++;
            } catch (Exception e) {
                errors.add("第 " + lineNo + " 行：" + e.getMessage());
            }
        }
        result.put("total", total);
        result.put("success", success);
        result.put("failure", total - success);
        result.put("errors", errors);
        return result;
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long toLong(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return Long.valueOf(text);
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value).trim());
    }
}
