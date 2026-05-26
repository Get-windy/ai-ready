package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_auxiliary_accounting")
public class AuxiliaryAccounting {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Integer auxiliaryType;
    
    private String auxiliaryCode;
    
    private String auxiliaryName;
    
    private Long refId;
    
    private String refCode;
    
    private String refName;
    
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
}