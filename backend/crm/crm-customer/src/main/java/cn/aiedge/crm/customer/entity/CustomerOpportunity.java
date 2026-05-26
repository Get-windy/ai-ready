package cn.aiedge.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("crm_customer_opportunity")
public class CustomerOpportunity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String opportunityCode;
    
    private String opportunityName;
    
    private Long customerId;
    
    private String customerName;
    
    private Long leadId;
    
    private Integer opportunityStage;
    
    private String opportunityStageDesc;
    
    private BigDecimal estimatedAmount;
    
    private BigDecimal actualAmount;
    
    private Integer probability;
    
    private Integer opportunityType;
    
    private Integer opportunitySource;
    
    private String productInterest;
    
    private String requirement;
    
    private String competitor;
    
    private String winReason;
    
    private String loseReason;
    
    private Integer status;
    
    private String statusDesc;
    
    private Long salesPersonId;
    
    private String salesPersonName;
    
    private Long departmentId;
    
    private String departmentName;
    
    private LocalDate expectedCloseDate;
    
    private LocalDate actualCloseDate;
    
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