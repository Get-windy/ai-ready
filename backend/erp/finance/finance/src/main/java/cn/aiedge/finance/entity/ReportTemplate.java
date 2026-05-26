package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_report_template")
public class ReportTemplate {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String templateCode;
    
    private String templateName;
    
    private Integer reportType;
    
    private Integer rowNo;
    
    private String rowCode;
    
    private String rowName;
    
    private String formula;
    
    private String subjectCodes;
    
    private Integer rowType;
    
    private Integer parentRowNo;
    
    private Integer level;
    
    private Integer displayOrder;
    
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