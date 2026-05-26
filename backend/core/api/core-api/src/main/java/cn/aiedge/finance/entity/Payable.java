package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应付账款实体
 */
@Data
@TableName("fin_payable")
public class Payable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String payableNo;
    private Long supplierId;
    private String supplierName;
    private Long contractId;
    private String contractNo;
    
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    
    private LocalDate billDate;
    private LocalDate dueDate;
    private Integer status; // 0-未付款 1-部分付款 2-已付款 3-已核销
    
    private Integer overdueDays;
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
