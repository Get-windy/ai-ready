package cn.aiedge.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_customer_lead")
public class CustomerLead {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String leadCode;
    
    private String leadName;
    
    private String contactName;
    
    private String contactPhone;
    
    private String contactEmail;
    
    private String companyName;
    
    private Integer industryType;
    
    private Integer leadSource;
    
    private Integer leadStatus;
    
    private String leadStatusDesc;
    
    private Integer leadLevel;
    
    private BigDecimal estimatedAmount;
    
    private String province;
    
    private String city;
    
    private String address;
    
    private String requirement;
    
    private String remark;
    
    private Long salesPersonId;
    
    private String salesPersonName;
    
    private Long departmentId;
    
    private String departmentName;
    
    private LocalDate expectedCloseDate;
    
    private LocalDate actualCloseDate;
    
    private Long convertedCustomerId;
    
    private LocalDateTime convertedTime;
    
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