package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录实体
 */
@Data
@TableName("fin_receipt")
public class Receipt {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String receiptNo;
    private Long receivableId;
    private Long customerId;
    private String customerName;
    
    private BigDecimal amount;
    private LocalDate receiptDate;
    private String paymentMethod; // 银行转账/现金/支票等
    private String bankAccount;
    private String bankName;
    
    private Integer status; // 0-待确认 1-已确认 2-已核销
    private String voucherNo;
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
