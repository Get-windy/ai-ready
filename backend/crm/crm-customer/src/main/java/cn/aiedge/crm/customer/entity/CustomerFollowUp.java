package cn.aiedge.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_customer_follow_up")
public class CustomerFollowUp {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String followUpCode;
    
    private Long customerId;
    
    private String customerName;
    
    private Long opportunityId;
    
    private String opportunityName;
    
    private Long leadId;
    
    private String leadName;
    
    private Integer followUpType;
    
    private String followUpTypeDesc;
    
    private String contactName;
    
    private String contactPhone;
    
    private LocalDate followUpDate;
    
    private String content;
    
    private String nextAction;
    
    private LocalDate nextFollowUpDate;
    
    private Integer followUpResult;
    
    private String followUpResultDesc;
    
    private Long salesPersonId;
    
    private String salesPersonName;
    
    private Long departmentId;
    
    private String departmentName;
    
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