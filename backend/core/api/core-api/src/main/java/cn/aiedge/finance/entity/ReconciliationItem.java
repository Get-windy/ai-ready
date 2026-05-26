package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账明细项实体
 */
@Data
@TableName("fin_reconciliation_item")
public class ReconciliationItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long reconciliationId; // 对账记录ID
    
    private String itemType; // PAYABLE-应付账款 RECEIVABLE-应收账款 PAYMENT-付款记录 RECEIPT-收款记录
    private Long itemId; // 明细项ID（对应的应付/应收/付款/收款ID）
    private String itemNo; // 明细项编号
    
    private BigDecimal systemAmount; // 系统记录金额
    private BigDecimal actualAmount; // 实际金额
    private BigDecimal difference; // 差异金额
    
    private Boolean matched; // 是否匹配
    private String differenceReason; // 差异原因
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
