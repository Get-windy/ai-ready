# CRM与ERP客户关系管理最佳实践设计文档

## 1. 业务概念澄清

### 1.1 CRM客户概念
- **定义**：包括潜在客户和实体客户的全部客户
- **范围**：针对客户维护及客户关系处理
- **性质**：即"公海客户"，是所有可能成为客户的客户和已经发生业务往来的实体客户在内的全部公海客户
- **用途**：客户开发、客户关系维护、商机跟进等

### 1.2 ERP客户概念（Partner）
- **定义**：特指与企业实际发生业务往来及交易的实体客户
- **范围**：实际的交易对象
- **性质**：在销售订单等实际业务流程中使用的客户
- **用途**：订单处理、交易记录、应收账款等

## 2. 实体关系模型设计

### 2.1 统一业务伙伴模型（推荐）
```
erp_partner (统一业务伙伴基表)
  ├── id: 主键
  ├── tenant_id: 租户ID
  ├── partner_code: 业务伙伴编码
  ├── partner_name: 业务伙伴名称
  ├── partner_type: BUSINESS / PERSON / GROUP
  ├── business_type: CUSTOMER / SUPPLIER / CUSTOMER_AND_SUPPLIER
  ├── customer_grade_code: 客户等级代码
  ├── customer_grade_name: 客户等级名称
  ├── contact_person: 联系人
  ├── contact_phone: 联系电话
  ├── contact_email: 联系邮箱
  ├── address: 地址
  ├── tax_no: 税号/统一社会信用代码
  ├── status: ACTIVE / INACTIVE / PROSPECT
  ├── lead_source: 线索来源 (CRM / MANUAL / IMPORT / ...)
  ├── crm_source_id: 来源CRM系统中的ID
  ├── crm_lead_id: 来源CRM线索ID
  ├── is_converted_from_lead: 是否从线索转化
  ├── converted_date: 转化日期
  ├── ext_info: 扩展信息(JSON)
  ├── deleted: 删除标记
  ├── create_time: 创建时间
  ├── update_time: 更新时间
  └── create_by/update_by: 创建/更新人

crm_erp_customer_mapping (CRM-ERP客户映射表)
  ├── id: 主键
  ├── tenant_id: 租户ID
  ├── crm_system: CRM系统标识 ('CRM_MODULE' / 'EXTERNAL_CRM' / ...)
  ├── crm_entity_type: CRM实体类型 ('CUSTOMER' / 'LEAD')
  ├── crm_id: CRM系统中的客户ID
  ├── erp_system: ERP系统标识 ('ERP_MODULE')
  ├── erp_entity_type: ERP实体类型 ('PARTNER')
  ├── erp_id: ERP系统中的业务伙伴ID
  ├── match_rule: 匹配规则 (TAX_NO / NAME_PHONE / MANUAL / ...)
  ├── match_confidence: 匹配置信度
  ├── sync_status: 同步状态 (PENDING / SYNCED / FAILED / MANUAL_OVERRIDE)
  ├── sync_time: 同步时间
  ├── ext_info: 扩展信息
  └── create_time: 创建时间
```

### 2.2 客户转化流程
```
1. 线索录入 (可能来自CRM模块或其他渠道)
   ↓
2. 客户资格确认 (是否符合实体客户标准)
   ↓
3. 业务伙伴创建 (在erp_partner表中创建记录)
   ↓
4. 映射关系建立 (在crm_erp_customer_mapping表中建立关联)
   ↓
5. 客户等级分配 (根据业务规则分配客户等级)
   ↓
6. 实体客户可用 (可用于销售订单等业务)
```

## 3. 匹配规则设计

### 3.1 客户去重匹配规则
| 规则 | 权重 | 说明 |
|------|------|------|
| 税号/统一社会信用代码精确匹配 | 1.0 | 法律唯一标识 |
| 公司名称完全一致 | 0.95 | 名称完全相同 |
| 电话/手机号匹配 | 0.85 | B2B场景电话可跨联系人 |
| 公司简称+城市匹配 | 0.80 | 考虑同名不同地 |
| 域名/邮箱后缀匹配 | 0.70 | 如@公司名.com |
| 公司名称编辑距离<3+城市匹配 | 0.65 | 容忍轻微名称差异 |

### 3.2 匹配逻辑
```java
/**
 * 客户匹配服务
 */
public interface CustomerMatchService {
    /**
     * 根据输入信息匹配已存在的实体客户
     * @param potentialCustomer 潜在客户信息
     * @return 匹配的实体客户，如果没找到返回null
     */
    Partner matchExistingCustomer(PotentialCustomerInfo potentialCustomer);
    
    /**
     * 创建新的实体客户
     * @param leadInfo 线索信息
     * @return 新创建的实体客户
     */
    Partner createEntityCustomerFromLead(CustomerLeadInfo leadInfo);
    
    /**
     * 建立CRM与ERP客户映射关系
     * @param mappingInfo 映射信息
     */
    void establishMapping(MappingInfo mappingInfo);
}
```

## 4. 数据一致性保障

### 4.1 同步策略
- **实时同步**：从CRM线索转为实体客户时，立即在ERP中创建Partner
- **异步补偿**：定时任务扫描未同步的数据，确保最终一致性
- **错误处理**：同步失败时记录错误日志，支持人工干预

### 4.2 数据验证
- 在创建实体客户前，验证税号、联系方式等关键信息的唯一性
- 确保客户等级的有效性
- 验证业务伙伴类型的正确性

## 5. 业务流程整合

### 5.1 销售订单中的客户使用
- 销售订单创建时，只能选择状态为ACTIVE的Partner作为客户
- 从Partner实体获取客户等级等相关信息
- 确保销售订单与实体客户之间的关联关系正确

### 5.2 客户管理界面
- 提供线索转化功能，将符合条件的线索转为实体客户
- 提供客户合并功能，处理重复客户
- 提供客户信息同步功能，确保信息一致性

## 6. 实施建议

### 6.1 渐进式实施
1. **第一阶段**：建立映射表和基础匹配逻辑
2. **第二阶段**：实现客户转化流程
3. **第三阶段**：完善数据验证和同步机制
4. **第四阶段**：提供管理界面和工具

### 6.2 数据迁移
- 将现有的客户数据迁移到新的Partner模型
- 建立历史数据的映射关系
- 验证数据迁移的准确性

## 7. 代码实现要点

### 7.1 Partner实体扩展
```java
@Entity
@Table(name = "erp_partner")
@Data
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long tenantId;
    private String partnerCode;  // 业务伙伴编码
    private String partnerName;  // 业务伙伴名称
    private String partnerType;  // BUSINESS / PERSON / GROUP
    private String businessType; // CUSTOMER / SUPPLIER / BOTH
    
    // 客户等级信息（用于销售订单定价）
    private String customerGradeCode;
    private String customerGradeName;
    
    // 联系信息
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    
    // 法律信息
    private String taxNo; // 税号/统一社会信用代码
    
    // 状态信息
    private String status; // ACTIVE / INACTIVE / PROSPECT
    private String leadSource; // 线索来源
    private Long crmSourceId; // 来源CRM ID
    private Long crmLeadId; // 来源CRM线索ID
    private Boolean isConvertedFromLead; // 是否从线索转化
    private Date convertedDate; // 转化日期
    
    @Column(name = "ext_info", columnDefinition = "jsonb")
    private String extInfo; // 扩展信息
    
    // 标准字段
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
}
```

### 7.2 映射服务实现
```java
@Service
@Transactional
public class CustomerMatchServiceImpl implements CustomerMatchService {
    
    @Autowired
    private PartnerService partnerService;
    
    @Autowired
    private CustomerMappingService mappingService;
    
    @Override
    public Partner matchExistingCustomer(PotentialCustomerInfo potentialCustomer) {
        // 1. 按税号精确匹配
        if (potentialCustomer.getTaxNo() != null) {
            Partner matched = partnerService.findByTaxNo(potentialCustomer.getTaxNo());
            if (matched != null) {
                return matched;
            }
        }
        
        // 2. 按名称和联系方式模糊匹配
        List<Partner> candidates = partnerService.findByNameAndContact(
            potentialCustomer.getName(),
            potentialCustomer.getContactPhone()
        );
        
        if (candidates.size() == 1) {
            // 单一匹配，返回结果
            return candidates.get(0);
        } else if (candidates.size() > 1) {
            // 多个候选，需要人工确认
            // 这里可以实现更复杂的匹配算法或返回多个候选
        }
        
        return null; // 没有找到匹配的客户
    }
    
    @Override
    public Partner createEntityCustomerFromLead(CustomerLeadInfo leadInfo) {
        // 验证输入数据
        validateLeadInfo(leadInfo);
        
        // 创建Partner实体
        Partner partner = new Partner();
        partner.setTenantId(leadInfo.getTenantId());
        partner.setPartnerName(leadInfo.getCompanyName());
        partner.setContactPerson(leadInfo.getContactPerson());
        partner.setContactPhone(leadInfo.getContactPhone());
        partner.setContactEmail(leadInfo.getContactEmail());
        partner.setTaxNo(leadInfo.getTaxNo());
        partner.setAddress(leadInfo.getAddress());
        partner.setStatus("ACTIVE"); // 直接激活为实体客户
        partner.setLeadSource(leadInfo.getSource());
        partner.setCrmSourceId(leadInfo.getCrmId());
        partner.setCrmLeadId(leadInfo.getId());
        partner.setIsConvertedFromLead(true);
        partner.setConvertedDate(new Date());
        partner.setCustomerGradeCode(determineCustomerGrade(leadInfo)); // 根据规则确定客户等级
        
        return partnerService.save(partner);
    }
    
    private String determineCustomerGrade(CustomerLeadInfo leadInfo) {
        // 根据业务规则确定客户等级
        // 这里可以根据客户的规模、行业、预期交易额等因素来确定等级
        return "NORMAL"; // 默认等级
    }
}
```

这个设计提供了一个完整的CRM到ERP客户关系管理解决方案，解决了客户转化的根本问题，确保了数据一致性，并提供了灵活的扩展能力。