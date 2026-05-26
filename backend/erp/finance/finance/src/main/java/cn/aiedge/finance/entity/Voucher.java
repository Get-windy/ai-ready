package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_voucher")
public class Voucher {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String voucherNo;
    
    private Integer voucherType;
    
    private String period;
    
    private LocalDate voucherDate;
    
    private Integer wordNo;
    
    private String word;
    
    private BigDecimal totalDebit;
    
    private BigDecimal totalCredit;
    
    private Integer entryCount;
    
    private Integer status;
    
    private Long preparedBy;
    
    private String preparedByName;
    
    private LocalDateTime preparedTime;
    
    private Long reviewedBy;
    
    private String reviewedByName;
    
    private LocalDateTime reviewedTime;
    
    private Long postedBy;
    
    private String postedByName;
    
    private LocalDateTime postedTime;
    
    private Long cashierId;
    
    private String cashierName;
    
    private String sourceType;
    
    private Long sourceId;
    
    private String sourceNo;
    
    private String remark;
    
    private Integer printed;
    
    private Integer printCount;
    
    private String voidReason;
    
    private Long voidBy;
    
    private LocalDateTime voidTime;
    
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
    private java.util.List<VoucherEntry> entries;
}