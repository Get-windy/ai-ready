package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款实体
 */
@Data
@TableName("fin_receivable")
public class Receivable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String receivableNo;
    private Long customerId;
    private String customerName;
    private Long contractId;
    private String contractNo;
    
    private BigDecimal originalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal remainingAmount;
    
    private LocalDate billDate;
    private LocalDate dueDate;
    private Integer status; // 0-未收款 1-部分收款 2-已收款 3-已核销
    
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
