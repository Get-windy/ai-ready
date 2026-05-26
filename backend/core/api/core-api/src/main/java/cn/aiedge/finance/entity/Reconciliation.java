package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录实体
 */
@Data
@TableName("fin_reconciliation")
public class Reconciliation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reconciliationNo;
    private String reconciliationType; // BANK-银行 CUSTOMER-客户 SUPPLIER-供应商
    
    private Long targetId; // 对方ID
    private String targetName;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate reconciliationDate;
    
    private BigDecimal systemBalance; // 系统余额
    private BigDecimal actualBalance; // 实际余额
    private BigDecimal difference; // 差异
    
    private Integer status; // 0-待对账 1-已对账 2-有差异
    private String differenceReason;
    private String handlerId;
    private String handlerName;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
