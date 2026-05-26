package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_cost_center")
public class CostCenter {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String centerCode;
    
    private String centerName;
    
    private Integer centerType;
    
    private Long parentId;
    
    private Integer level;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long projectId;
    
    private String projectName;
    
    private Long productId;
    
    private String productName;
    
    private Integer allocationMethod;
    
    private BigDecimal allocationRate;
    
    private String allocationBase;
    
    private Integer enabled;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    private Long tenantId;
    
    @Version
    private Integer version;
    
    @TableField(exist = false)
    private java.util.List<CostCenter> children;
}