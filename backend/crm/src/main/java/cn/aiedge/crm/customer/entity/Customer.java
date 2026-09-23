package cn.aiedge.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_customer")
public class Customer {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String customerCode;
    
    private String customerName;
    
    private String shortName;
    
    private Integer customerType;
    
    private Integer customerSource;
    
    private Integer industryType;
    
    private String province;
    
    private String city;
    
    private String district;
    
    private String address;
    
    private String phone;
    
    private String fax;
    
    private String email;
    
    private String website;
    
    private String legalPerson;
    
    private String businessContact;
    
    private String businessContactPhone;
    
    private String financeContact;
    
    private String financeContactPhone;
    
    private String taxNumber;
    
    private String bankName;
    
    private String bankAccount;
    
    private Integer customerLevel;
    
    private String customerLevelDesc;
    
    private BigDecimal creditLimit;
    
    private BigDecimal currentDebt;
    
    private Integer settlementType;
    
    private Integer settlementDays;
    
    private Integer status;
    
    private String statusDesc;
    
    private Long salesPersonId;
    
    private String salesPersonName;
    
    private Long departmentId;
    
    private String departmentName;
    
    private LocalDate firstTradeDate;
    
    private LocalDate lastTradeDate;
    
    private Integer tradeCount;
    
    private BigDecimal tradeAmount;
    
    private BigDecimal potentialAmount;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.UPDATE)
    private String updatedBy;
    
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;

    /**
     * 关联的 ERP 往来单位 ID（{@code biz_party.id}）。
     *
     * <p>本模块 README 的红线要求：CRM 客户只是「可能客户」，真正的交易主体是 ERP 往来单位，
     * 业务单据（销售订单等）的 {@code customer_id} 必须写 ERP 往来单位 ID。
     * 该列是 CRM 客户 → ERP 往来单位的唯一映射位；为空表示尚未建档到往来单位，
     * 此时任何「转订单」类动作都必须**拒绝**，而不是把 CRM 客户 ID 直接写进 ERP 单据。</p>
     */
    private Long mdPartnerId;
}