package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录实体
 */
@Data
@TableName("fin_payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String paymentNo;
    private Long payableId;
    private Long supplierId;
    private String supplierName;
    
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String bankAccount;
    private String bankName;
    
    private Integer status; // 0-待审批 1-已审批 2-已支付
    private String voucherNo;
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
