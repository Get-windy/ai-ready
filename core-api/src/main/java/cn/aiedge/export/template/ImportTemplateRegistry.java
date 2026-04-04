package cn.aiedge.export.template;

import cn.aiedge.export.template.ImportTemplateDefinition.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入模板注册表
 * 管理所有导入模板的定义
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
public class ImportTemplateRegistry {

    private final Map<String, ImportTemplateDefinition> templates = new ConcurrentHashMap<>();
    private final Map<String, String> dataTypeToTemplate = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册默认模板
        registerDefaultTemplates();
        log.info("导入模板注册完成，共{}个模板", templates.size());
    }

    /**
     * 注册模板
     */
    public void register(ImportTemplateDefinition template) {
        if (template == null || template.getTemplateId() == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        templates.put(template.getTemplateId(), template);
        dataTypeToTemplate.put(template.getDataType(), template.getTemplateId());
        
        log.info("注册导入模板: templateId={}, dataType={}", template.getTemplateId(), template.getDataType());
    }

    /**
     * 根据模板ID获取模板
     */
    public ImportTemplateDefinition getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /**
     * 根据数据类型获取模板
     */
    public ImportTemplateDefinition getTemplateByDataType(String dataType) {
        String templateId = dataTypeToTemplate.get(dataType);
        return templateId != null ? templates.get(templateId) : null;
    }

    /**
     * 获取所有模板
     */
    public List<ImportTemplateDefinition> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /**
     * 获取所有数据类型
     */
    public Set<String> getSupportedDataTypes() {
        return new HashSet<>(dataTypeToTemplate.keySet());
    }

    /**
     * 移除模板
     */
    public boolean removeTemplate(String templateId) {
        ImportTemplateDefinition removed = templates.remove(templateId);
        if (removed != null) {
            dataTypeToTemplate.remove(removed.getDataType());
            return true;
        }
        return false;
    }

    /**
     * 更新模板
     */
    public void updateTemplate(ImportTemplateDefinition template) {
        if (template == null || template.getTemplateId() == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        
        if (!templates.containsKey(template.getTemplateId())) {
            throw new IllegalArgumentException("模板不存在: " + template.getTemplateId());
        }
        
        template.setUpdateTime(LocalDateTime.now());
        templates.put(template.getTemplateId(), template);
    }

    // ==================== 默认模板定义 ====================

    private void registerDefaultTemplates() {
        // 用户导入模板
        register(createUserTemplate());
        
        // 客户导入模板
        register(createCustomerTemplate());
        
        // 产品导入模板
        register(createProductTemplate());
        
        // 订单导入模板
        register(createOrderTemplate());
    }

    private ImportTemplateDefinition createUserTemplate() {
        return ImportTemplateDefinition.builder()
                .templateId("tpl_user_import")
                .dataType("user")
                .templateName("用户导入模板")
                .description("批量导入系统用户数据")
                .version("1.0.0")
                .fields(Arrays.asList(
                    TemplateField.builder()
                        .fieldName("username").fieldTitle("用户名")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(50).unique(true)
                        .sampleValue("zhangsan")
                        .description("登录用户名，唯一标识")
                        .order(1).build(),
                    TemplateField.builder()
                        .fieldName("email").fieldTitle("邮箱")
                        .fieldType(FieldType.EMAIL).required(true)
                        .sampleValue("zhangsan@example.com")
                        .description("用户邮箱地址")
                        .order(2).build(),
                    TemplateField.builder()
                        .fieldName("phone").fieldTitle("手机号")
                        .fieldType(FieldType.PHONE).required(false)
                        .sampleValue("13800138000")
                        .regexPattern("^1[3-9]\\d{9}$")
                        .description("11位手机号码")
                        .order(3).build(),
                    TemplateField.builder()
                        .fieldName("realName").fieldTitle("真实姓名")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(20)
                        .sampleValue("张三")
                        .order(4).build(),
                    TemplateField.builder()
                        .fieldName("deptId").fieldTitle("部门ID")
                        .fieldType(FieldType.REFERENCE).required(false)
                        .referenceType("department")
                        .description("所属部门ID")
                        .order(5).build(),
                    TemplateField.builder()
                        .fieldName("status").fieldTitle("状态")
                        .fieldType(FieldType.ENUM).required(true)
                        .dropdownOptions(Map.of("0", "禁用", "1", "启用"))
                        .defaultValue("1")
                        .sampleValue("1")
                        .order(6).build()
                ))
                .sampleRowCount(3)
                .maxImportRows(10000)
                .strictValidation(true)
                .build();
    }

    private ImportTemplateDefinition createCustomerTemplate() {
        return ImportTemplateDefinition.builder()
                .templateId("tpl_customer_import")
                .dataType("customer")
                .templateName("客户导入模板")
                .description("批量导入客户数据")
                .version("1.0.0")
                .fields(Arrays.asList(
                    TemplateField.builder()
                        .fieldName("customerName").fieldTitle("客户名称")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(100)
                        .sampleValue("测试公司")
                        .order(1).build(),
                    TemplateField.builder()
                        .fieldName("contact").fieldTitle("联系人")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(50)
                        .sampleValue("李经理")
                        .order(2).build(),
                    TemplateField.builder()
                        .fieldName("phone").fieldTitle("联系电话")
                        .fieldType(FieldType.PHONE).required(true)
                        .sampleValue("13800138000")
                        .order(3).build(),
                    TemplateField.builder()
                        .fieldName("email").fieldTitle("邮箱")
                        .fieldType(FieldType.EMAIL).required(false)
                        .sampleValue("contact@test.com")
                        .order(4).build(),
                    TemplateField.builder()
                        .fieldName("address").fieldTitle("地址")
                        .fieldType(FieldType.STRING).required(false)
                        .maxLength(200)
                        .sampleValue("北京市朝阳区xxx")
                        .order(5).build(),
                    TemplateField.builder()
                        .fieldName("customerType").fieldTitle("客户类型")
                        .fieldType(FieldType.ENUM).required(true)
                        .dropdownOptions(Map.of("1", "企业客户", "2", "个人客户", "3", "VIP客户"))
                        .defaultValue("1")
                        .order(6).build(),
                    TemplateField.builder()
                        .fieldName("remark").fieldTitle("备注")
                        .fieldType(FieldType.STRING).required(false)
                        .maxLength(500)
                        .order(7).build()
                ))
                .sampleRowCount(3)
                .maxImportRows(5000)
                .strictValidation(true)
                .build();
    }

    private ImportTemplateDefinition createProductTemplate() {
        return ImportTemplateDefinition.builder()
                .templateId("tpl_product_import")
                .dataType("product")
                .templateName("产品导入模板")
                .description("批量导入产品数据")
                .version("1.0.0")
                .fields(Arrays.asList(
                    TemplateField.builder()
                        .fieldName("productCode").fieldTitle("产品编码")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(50).unique(true)
                        .sampleValue("PROD-001")
                        .order(1).build(),
                    TemplateField.builder()
                        .fieldName("productName").fieldTitle("产品名称")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(100)
                        .sampleValue("智能手表")
                        .order(2).build(),
                    TemplateField.builder()
                        .fieldName("category").fieldTitle("分类")
                        .fieldType(FieldType.ENUM).required(true)
                        .dropdownOptions(Map.of("electronics", "电子产品", "clothing", "服装", "food", "食品", "other", "其他"))
                        .order(3).build(),
                    TemplateField.builder()
                        .fieldName("price").fieldTitle("价格")
                        .fieldType(FieldType.DECIMAL).required(true)
                        .minValue(0.0).maxValue(999999.99)
                        .sampleValue("299.00")
                        .order(4).build(),
                    TemplateField.builder()
                        .fieldName("stock").fieldTitle("库存")
                        .fieldType(FieldType.INTEGER).required(true)
                        .minValue(0.0)
                        .sampleValue("1000")
                        .order(5).build(),
                    TemplateField.builder()
                        .fieldName("status").fieldTitle("状态")
                        .fieldType(FieldType.ENUM).required(true)
                        .dropdownOptions(Map.of("1", "上架", "0", "下架"))
                        .defaultValue("1")
                        .order(6).build(),
                    TemplateField.builder()
                        .fieldName("description").fieldTitle("描述")
                        .fieldType(FieldType.STRING).required(false)
                        .maxLength(1000)
                        .order(7).build()
                ))
                .sampleRowCount(3)
                .maxImportRows(20000)
                .strictValidation(true)
                .build();
    }

    private ImportTemplateDefinition createOrderTemplate() {
        return ImportTemplateDefinition.builder()
                .templateId("tpl_order_import")
                .dataType("order")
                .templateName("订单导入模板")
                .description("批量导入订单数据")
                .version("1.0.0")
                .fields(Arrays.asList(
                    TemplateField.builder()
                        .fieldName("orderNo").fieldTitle("订单号")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(50).unique(true)
                        .sampleValue("ORD-20260404-001")
                        .order(1).build(),
                    TemplateField.builder()
                        .fieldName("customerName").fieldTitle("客户名称")
                        .fieldType(FieldType.STRING).required(true)
                        .maxLength(100)
                        .order(2).build(),
                    TemplateField.builder()
                        .fieldName("productCode").fieldTitle("产品编码")
                        .fieldType(FieldType.REFERENCE).required(true)
                        .referenceType("product")
                        .order(3).build(),
                    TemplateField.builder()
                        .fieldName("quantity").fieldTitle("数量")
                        .fieldType(FieldType.INTEGER).required(true)
                        .minValue(1.0).maxValue(9999.0)
                        .sampleValue("10")
                        .order(4).build(),
                    TemplateField.builder()
                        .fieldName("unitPrice").fieldTitle("单价")
                        .fieldType(FieldType.DECIMAL).required(true)
                        .minValue(0.01)
                        .sampleValue("299.00")
                        .order(5).build(),
                    TemplateField.builder()
                        .fieldName("orderDate").fieldTitle("订单日期")
                        .fieldType(FieldType.DATE).required(true)
                        .sampleValue("2026-04-04")
                        .order(6).build(),
                    TemplateField.builder()
                        .fieldName("remark").fieldTitle("备注")
                        .fieldType(FieldType.STRING).required(false)
                        .maxLength(500)
                        .order(7).build()
                ))
                .sampleRowCount(3)
                .maxImportRows(50000)
                .strictValidation(true)
                .build();
    }
}
