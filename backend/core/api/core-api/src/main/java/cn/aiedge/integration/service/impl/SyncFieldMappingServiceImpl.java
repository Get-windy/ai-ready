package cn.aiedge.integration.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.integration.mapper.SyncFieldMappingMapper;
import cn.aiedge.integration.model.SyncFieldMapping;
import cn.aiedge.integration.service.SyncFieldMappingService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 同步字段映射服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SyncFieldMappingServiceImpl implements SyncFieldMappingService {

    private final SyncFieldMappingMapper fieldMappingMapper;

    private Long getCurrentTenantId() {
        try {
            Object tenantId = StpUtil.getSession().get("tenantId");
            if (tenantId instanceof Number) {
                return ((Number) tenantId).longValue();
            }
        } catch (Exception e) {
            // 忽略未登录异常
        }
        return 1L;
    }

    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public List<SyncFieldMapping> listByConfigId(Long configId) {
        return fieldMappingMapper.selectByConfigId(configId);
    }

    @Override
    public List<SyncFieldMapping> listByConfigAndBillType(Long configId, String billType) {
        return fieldMappingMapper.selectByConfigAndBillType(configId, billType);
    }

    @Override
    public List<SyncFieldMapping> batchSave(Long configId, List<SyncFieldMapping> mappings) {
        // 先删除旧映射
        fieldMappingMapper.deleteByConfigId(configId);

        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }

        Long tenantId = getCurrentTenantId();
        Long userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<SyncFieldMapping> saved = new ArrayList<>();
        int sortOrder = 0;
        for (SyncFieldMapping mapping : mappings) {
            mapping.setId(null);
            mapping.setTenantId(tenantId);
            mapping.setSourceConfigId(configId);
            mapping.setStatus(1);
            mapping.setDeleted(0);
            mapping.setSortOrder(sortOrder++);
            mapping.setCreateBy(userId);
            mapping.setUpdateBy(userId);
            mapping.setCreateTime(now);
            mapping.setUpdateTime(now);
            fieldMappingMapper.insert(mapping);
            saved.add(mapping);
        }

        log.info("批量保存字段映射: configId={}, count={}", configId, saved.size());
        return saved;
    }

    @Override
    public SyncFieldMapping create(SyncFieldMapping mapping) {
        Long tenantId = getCurrentTenantId();
        mapping.setTenantId(tenantId);
        mapping.setCreateBy(getCurrentUserId());
        mapping.setUpdateBy(getCurrentUserId());
        fieldMappingMapper.insert(mapping);
        log.info("创建字段映射: id={}, configId={}, billType={}", mapping.getId(), mapping.getSourceConfigId(), mapping.getBillType());
        return mapping;
    }

    @Override
    public SyncFieldMapping update(Long id, SyncFieldMapping mapping) {
        SyncFieldMapping existing = fieldMappingMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("字段映射不存在");
        }

        if (mapping.getSourceField() != null) existing.setSourceField(mapping.getSourceField());
        if (mapping.getSourceLabel() != null) existing.setSourceLabel(mapping.getSourceLabel());
        if (mapping.getTargetField() != null) existing.setTargetField(mapping.getTargetField());
        if (mapping.getTargetLabel() != null) existing.setTargetLabel(mapping.getTargetLabel());
        if (mapping.getTransformType() != null) existing.setTransformType(mapping.getTransformType());
        if (mapping.getTransformRule() != null) existing.setTransformRule(mapping.getTransformRule());
        if (mapping.getDefaultValue() != null) existing.setDefaultValue(mapping.getDefaultValue());
        if (mapping.getRequired() != null) existing.setRequired(mapping.getRequired());
        if (mapping.getSortOrder() != null) existing.setSortOrder(mapping.getSortOrder());
        if (mapping.getStatus() != null) existing.setStatus(mapping.getStatus());

        existing.setUpdateBy(getCurrentUserId());
        existing.setUpdateTime(LocalDateTime.now());

        fieldMappingMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        SyncFieldMapping existing = fieldMappingMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        fieldMappingMapper.deleteById(id);
        return true;
    }

    @Override
    public List<SyncFieldMapping> initFromTemplate(Long configId, String billType) {
        List<SyncFieldMapping> template = getTemplateForBillType(billType);
        if (template.isEmpty()) {
            throw new BusinessException("单据类型 " + billType + " 没有预置模板");
        }

        // 查找当前配置下该单据类型的现有映射
        LambdaQueryWrapper<SyncFieldMapping> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SyncFieldMapping::getSourceConfigId, configId);
        wrapper.eq(SyncFieldMapping::getBillType, billType);
        wrapper.eq(SyncFieldMapping::getDeleted, 0);
        Long count = fieldMappingMapper.selectCount(wrapper);

        if (count > 0) {
            throw new BusinessException("单据类型 " + billType + " 已有字段映射，请先删除再初始化");
        }

        Long tenantId = getCurrentTenantId();
        Long userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int sortOrder = 0;

        List<SyncFieldMapping> saved = new ArrayList<>();
        for (SyncFieldMapping t : template) {
            SyncFieldMapping m = new SyncFieldMapping();
            m.setTenantId(tenantId);
            m.setSourceConfigId(configId);
            m.setBillType(billType);
            m.setSourceField(t.getSourceField());
            m.setSourceLabel(t.getSourceLabel());
            m.setTargetField(t.getTargetField());
            m.setTargetLabel(t.getTargetLabel());
            m.setTransformType(t.getTransformType());
            m.setTransformRule(t.getTransformRule());
            m.setDefaultValue(t.getDefaultValue());
            m.setRequired(t.getRequired());
            m.setSortOrder(sortOrder++);
            m.setStatus(1);
            m.setDeleted(0);
            m.setCreateBy(userId);
            m.setUpdateBy(userId);
            m.setCreateTime(now);
            m.setUpdateTime(now);
            fieldMappingMapper.insert(m);
            saved.add(m);
        }

        log.info("初始化字段映射模板: configId={}, billType={}, count={}", configId, billType, saved.size());
        return saved;
    }

    // ==================== 预置模板 ====================

    /**
     * 根据单据类型返回预置的字段映射模板
     */
    private List<SyncFieldMapping> getTemplateForBillType(String billType) {
        return switch (billType) {
            case "601" -> customerTemplate();   // 客户
            case "604" -> supplierTemplate();   // 供应商
            case "504" -> productTemplate();    // 商品
            case "801" -> inventoryTemplate();  // 库存
            case "101", "102", "103" -> salesTemplate(billType);     // 销售
            case "201", "202", "203" -> purchaseTemplate(billType);  // 采购
            case "301", "302", "303", "304" -> financeTemplate(billType); // 财务
            default -> List.of();
        };
    }

    /**
     * 客户字段映射模板 (601)
     * 外部系统(来肯) → 本地 Party (partyType=1 客户)
     */
    private List<SyncFieldMapping> customerTemplate() {
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("customer_code").setSourceLabel("客户编码")
                .setTargetField("partyCode").setTargetLabel("往来单位编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("customer_name").setSourceLabel("客户名称")
                .setTargetField("partyName").setTargetLabel("往来单位名称")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("short_name").setSourceLabel("简称")
                .setTargetField("shortName").setTargetLabel("简称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("contact_person").setSourceLabel("联系人")
                .setTargetField("contact").setTargetLabel("联系人")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("phone").setSourceLabel("电话")
                .setTargetField("phone").setTargetLabel("联系电话")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("address").setSourceLabel("地址")
                .setTargetField("address").setTargetLabel("地址")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("settlement_type").setSourceLabel("结算方式")
                .setTargetField("settlementType").setTargetLabel("结算方式")
                .setTransformType("enum")
                .setTransformRule("{\"现金\":0,\"挂账\":1}")
                .setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("remark").setSourceLabel("备注")
                .setTargetField("remark").setTargetLabel("备注")
                .setTransformType("direct").setRequired(false));
        // 固定值：往来类型=客户(1)
        list.add(new SyncFieldMapping()
                .setSourceField("").setSourceLabel("")
                .setTargetField("partyType").setTargetLabel("往来类型")
                .setTransformType("default").setDefaultValue("1").setRequired(true));
        return list;
    }

    /**
     * 供应商字段映射模板 (604)
     */
    private List<SyncFieldMapping> supplierTemplate() {
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("supplier_code").setSourceLabel("供应商编码")
                .setTargetField("partyCode").setTargetLabel("往来单位编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("supplier_name").setSourceLabel("供应商名称")
                .setTargetField("partyName").setTargetLabel("往来单位名称")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("short_name").setSourceLabel("简称")
                .setTargetField("shortName").setTargetLabel("简称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("contact_person").setSourceLabel("联系人")
                .setTargetField("contact").setTargetLabel("联系人")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("phone").setSourceLabel("电话")
                .setTargetField("phone").setTargetLabel("联系电话")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("address").setSourceLabel("地址")
                .setTargetField("address").setTargetLabel("地址")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("remark").setSourceLabel("备注")
                .setTargetField("remark").setTargetLabel("备注")
                .setTransformType("direct").setRequired(false));
        // 固定值：往来类型=供应商(2)
        list.add(new SyncFieldMapping()
                .setSourceField("").setSourceLabel("")
                .setTargetField("partyType").setTargetLabel("往来类型")
                .setTransformType("default").setDefaultValue("2").setRequired(true));
        return list;
    }

    /**
     * 商品字段映射模板 (504)
     */
    private List<SyncFieldMapping> productTemplate() {
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("product_code").setSourceLabel("商品编码")
                .setTargetField("productCode").setTargetLabel("商品编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("product_name").setSourceLabel("商品名称")
                .setTargetField("productName").setTargetLabel("商品名称")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("barcode").setSourceLabel("条码")
                .setTargetField("barcode").setTargetLabel("条码")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("unit").setSourceLabel("单位")
                .setTargetField("unit").setTargetLabel("单位")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("category").setSourceLabel("分类")
                .setTargetField("categoryName").setTargetLabel("商品分类")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("cost_price").setSourceLabel("成本价")
                .setTargetField("costPrice").setTargetLabel("成本价")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("sale_price").setSourceLabel("零售价")
                .setTargetField("salePrice").setTargetLabel("零售价")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("spec").setSourceLabel("规格")
                .setTargetField("spec").setTargetLabel("规格")
                .setTransformType("direct").setRequired(false));
        return list;
    }

    /**
     * 库存字段映射模板 (801)
     */
    private List<SyncFieldMapping> inventoryTemplate() {
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("product_code").setSourceLabel("商品编码")
                .setTargetField("productCode").setTargetLabel("商品编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("warehouse_code").setSourceLabel("仓库编码")
                .setTargetField("warehouseCode").setTargetLabel("仓库编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("quantity").setSourceLabel("库存数量")
                .setTargetField("quantity").setTargetLabel("库存数量")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("batch_no").setSourceLabel("批次号")
                .setTargetField("batchNo").setTargetLabel("批次号")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("production_date").setSourceLabel("生产日期")
                .setTargetField("productionDate").setTargetLabel("生产日期")
                .setTransformType("direct").setRequired(false));
        return list;
    }

    /**
     * 销售类单据字段映射模板 (101=销售订单, 102=销售出库, 103=销售退货)
     */
    private List<SyncFieldMapping> salesTemplate(String billType) {
        String billLabel = switch (billType) {
            case "101" -> "销售订单";
            case "102" -> "销售出库单";
            case "103" -> "销售退货单";
            default -> "销售单据";
        };
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("bill_no").setSourceLabel(billLabel + "编号")
                .setTargetField("billNo").setTargetLabel("单据编号")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("bill_date").setSourceLabel("单据日期")
                .setTargetField("billDate").setTargetLabel("单据日期")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("customer_code").setSourceLabel("客户编码")
                .setTargetField("customerCode").setTargetLabel("客户编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("customer_name").setSourceLabel("客户名称")
                .setTargetField("customerName").setTargetLabel("客户名称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("product_code").setSourceLabel("商品编码")
                .setTargetField("productCode").setTargetLabel("商品编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("product_name").setSourceLabel("商品名称")
                .setTargetField("productName").setTargetLabel("商品名称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("quantity").setSourceLabel("数量")
                .setTargetField("quantity").setTargetLabel("数量")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("unit_price").setSourceLabel("单价")
                .setTargetField("unitPrice").setTargetLabel("单价")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("amount").setSourceLabel("金额")
                .setTargetField("amount").setTargetLabel("金额")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("warehouse_code").setSourceLabel("仓库编码")
                .setTargetField("warehouseCode").setTargetLabel("仓库编码")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("remark").setSourceLabel("备注")
                .setTargetField("remark").setTargetLabel("备注")
                .setTransformType("direct").setRequired(false));
        return list;
    }

    /**
     * 采购类单据字段映射模板 (201=采购订单, 202=采购入库, 203=采购退货)
     */
    private List<SyncFieldMapping> purchaseTemplate(String billType) {
        String billLabel = switch (billType) {
            case "201" -> "采购订单";
            case "202" -> "采购入库单";
            case "203" -> "采购退货单";
            default -> "采购单据";
        };
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("bill_no").setSourceLabel(billLabel + "编号")
                .setTargetField("billNo").setTargetLabel("单据编号")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("bill_date").setSourceLabel("单据日期")
                .setTargetField("billDate").setTargetLabel("单据日期")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("supplier_code").setSourceLabel("供应商编码")
                .setTargetField("supplierCode").setTargetLabel("供应商编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("supplier_name").setSourceLabel("供应商名称")
                .setTargetField("supplierName").setTargetLabel("供应商名称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("product_code").setSourceLabel("商品编码")
                .setTargetField("productCode").setTargetLabel("商品编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("product_name").setSourceLabel("商品名称")
                .setTargetField("productName").setTargetLabel("商品名称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("quantity").setSourceLabel("数量")
                .setTargetField("quantity").setTargetLabel("数量")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("unit_price").setSourceLabel("单价")
                .setTargetField("unitPrice").setTargetLabel("单价")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("amount").setSourceLabel("金额")
                .setTargetField("amount").setTargetLabel("金额")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("warehouse_code").setSourceLabel("仓库编码")
                .setTargetField("warehouseCode").setTargetLabel("仓库编码")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("remark").setSourceLabel("备注")
                .setTargetField("remark").setTargetLabel("备注")
                .setTransformType("direct").setRequired(false));
        return list;
    }

    /**
     * 财务类单据字段映射模板 (301=应收, 302=应付, 303=收款单, 304=付款单)
     */
    private List<SyncFieldMapping> financeTemplate(String billType) {
        String billLabel = switch (billType) {
            case "301" -> "应收账款";
            case "302" -> "应付账款";
            case "303" -> "收款单";
            case "304" -> "付款单";
            default -> "财务单据";
        };
        List<SyncFieldMapping> list = new ArrayList<>();
        list.add(new SyncFieldMapping()
                .setSourceField("bill_no").setSourceLabel(billLabel + "编号")
                .setTargetField("billNo").setTargetLabel("单据编号")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("bill_date").setSourceLabel("单据日期")
                .setTargetField("billDate").setTargetLabel("单据日期")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("party_code").setSourceLabel("往来单位编码")
                .setTargetField("partyCode").setTargetLabel("往来单位编码")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("party_name").setSourceLabel("往来单位名称")
                .setTargetField("partyName").setTargetLabel("往来单位名称")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("amount").setSourceLabel("金额")
                .setTargetField("amount").setTargetLabel("金额")
                .setTransformType("direct").setRequired(true));
        list.add(new SyncFieldMapping()
                .setSourceField("balance").setSourceLabel("余额")
                .setTargetField("balance").setTargetLabel("余额")
                .setTransformType("direct").setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("settlement_type").setSourceLabel("结算方式")
                .setTargetField("settlementType").setTargetLabel("结算方式")
                .setTransformType("enum")
                .setTransformRule("{\"现金\":0,\"挂账\":1}")
                .setRequired(false));
        list.add(new SyncFieldMapping()
                .setSourceField("remark").setSourceLabel("备注")
                .setTargetField("remark").setTargetLabel("备注")
                .setTransformType("direct").setRequired(false));
        return list;
    }
}
