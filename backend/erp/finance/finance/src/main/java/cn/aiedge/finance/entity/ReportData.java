package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_report_data")
public class ReportData {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long reportId;
    
    private Integer rowNo;
    
    private String rowCode;
    
    private String rowName;
    
    private BigDecimal currentAmount;
    
    private BigDecimal previousAmount;
    
    private BigDecimal yearAmount;
    
    private String formula;
    
    private Integer rowType;
    
    private Integer parentRowNo;
    
    private Integer level;
    
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