package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_voucher_entry")
public class VoucherEntry {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long voucherId;
    
    private Integer entryNo;
    
    private String summary;
    
    private Long subjectId;
    
    private String subjectCode;
    
    private String subjectName;
    
    private BigDecimal debitAmount;
    
    private BigDecimal creditAmount;
    
    private Integer auxiliaryFlag;
    
    private Long auxiliaryId1;
    
    private Integer auxiliaryType1;
    
    private String auxiliaryValue1;
    
    private Long auxiliaryId2;
    
    private Integer auxiliaryType2;
    
    private String auxiliaryValue2;
    
    private Long auxiliaryId3;
    
    private Integer auxiliaryType3;
    
    private String auxiliaryValue3;
    
    private Long auxiliaryId4;
    
    private Integer auxiliaryType4;
    
    private String auxiliaryValue4;
    
    private Long auxiliaryId5;
    
    private Integer auxiliaryType5;
    
    private String auxiliaryValue5;
    
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