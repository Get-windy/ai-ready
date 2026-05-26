package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_asset_category")
public class AssetCategory {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String categoryCode;
    
    private String categoryName;
    
    private Long parentId;
    
    private Integer level;
    
    private Integer depreciationMethod;
    
    private Integer defaultUsefulLife;
    
    private BigDecimal defaultResidualRate;
    
    private Long subjectId;
    
    private String subjectCode;
    
    private Long depreciationSubjectId;
    
    private String depreciationSubjectCode;
    
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
    private java.util.List<AssetCategory> children;
}