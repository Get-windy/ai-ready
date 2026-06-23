# 客户信息处理逻辑修正专项说明

## 问题背景

在销售订单表单重构项目中，我们发现了一个重要的业务概念混淆问题：
- **CRM模块的客户**：指的是潜在客户，即"公海客户"，是所有可能成为客户的客户
- **ERP模块的客户**：指的是实体客户，即与租户发生实际业务往来及交易的客户

在初始开发中，我们错误地使用了CRM模块的客户信息来处理ERP模块的销售订单，这是不符合业务逻辑的。

## 问题分析

### 原始错误实现
1. 销售订单中使用CRM Customer实体获取客户信息
2. 在pom.xml中依赖了`erp-customer`模块
3. 客户等级信息从CRM客户体系获取

### 业务逻辑冲突
- CRM客户主要用于客户开发和转化过程
- ERP客户用于实际的业务交易和订单处理
- 混淆这两个概念会导致数据模型和业务流程不一致

## 解决方案

### 1. 更换客户实体
- 从CRM Customer实体更换为ERP Partner实体
- CRM Customer实体包含公海客户（潜在客户和实体客户的总体）
- Partner实体包含了ERP模块中的实际发生业务往来的实体客户信息
- Partner实体包含客户等级代码和名称等关键字段

### 2. 依赖模块调整
- 移除`erp-customer`依赖
- 添加`erp-partner`依赖
- 确保依赖关系正确

### 3. 服务调用修正
- 从CustomerService更换为PartnerService
- 保持API调用方式的一致性
- 确保功能逻辑不受影响

## 技术实现

### 实体变更
```java
// 从CRM Customer实体
import cn.aiedge.crm.customer.entity.Customer;

// 更改为ERP Partner实体
import cn.aiedge.erp.partner.entity.Partner;
```

### 服务注入变更
```java
// 从CRM CustomerService
private final CustomerService customerService;

// 更改为PartnerService
private final PartnerService partnerService;
```

### 客户等级获取逻辑修正
```java
private String getCustomerGradeCode(Long customerId) {
    if (customerId == null) return null;

    try {
        // 通过Partner实体获取客户等级信息
        Partner partner = partnerService.getById(customerId);
        if (partner != null && partner.getCustomerGradeCode() != null) {
            return partner.getCustomerGradeCode();
        }

        // 如果Partner中没有客户等级信息，尝试从客户等级服务中获取默认等级
        CustomerGrade defaultGrade = customerGradeService.getDefaultGrade();
        return defaultGrade != null ? defaultGrade.getGradeCode() : "DEFAULT";
    } catch (Exception e) {
        log.warn("获取客户等级信息失败，使用默认等级: customerId={}, error={}", customerId, e.getMessage());
        return "DEFAULT";
    }
}
```

## 业务意义

### 1. 概念清晰
- CRM模块专注于客户获取和培育
- ERP模块专注于实际业务交易
- 两个模块各司其职，概念边界清晰

### 2. 数据一致性
- 销售订单基于实际的ERP客户进行处理
- 避免了潜在客户与实际客户的混淆
- 确保了业务数据的准确性

### 3. 流程合规
- 符合企业实际的业务流程
- CRM将潜在客户转化为ERP实体客户
- 销售订单基于实体客户进行处理

## 验证方法

### 功能测试
1. 创建销售订单时，确保客户信息正确从Partner实体获取
2. 客户等级信息正确应用于价格计算
3. 订单处理流程正常执行

### 数据验证
1. 验证客户ID对应的是ERP Partner而非CRM Customer
2. 检查客户等级信息的准确性和完整性
3. 确认价格计算基于正确的客户等级

## 影响评估

### 正面影响
- 业务逻辑更加清晰和准确
- 数据模型符合实际业务场景
- 系统架构更加合理

### 风险控制
- 所有更改都在后端服务层，不影响前端界面
- 保持了API接口的兼容性
- 通过依赖注入实现平滑过渡

## 总结

通过这次修正，我们不仅解决了技术实现上的问题，更重要的是确保了系统设计符合实际的业务逻辑。CRM和ERP模块的概念区分在企业信息系统中非常重要，这种区分确保了系统能够正确地支持客户全生命周期的管理，从潜在客户开发到实际业务交易的完整流程。