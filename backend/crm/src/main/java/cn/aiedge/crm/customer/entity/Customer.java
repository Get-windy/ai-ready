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
}