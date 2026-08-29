package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水台账实体
 * 记录所有收付款资金变动
 */
@Data
@Accessors(chain = true)
@TableName("erp_capital_flow")
public class CapitalFlow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String flowNo;

    private String flowType;

    private String direction;

    private Long refId;

    private String refNo;

    private String refType;

    private BigDecimal amount;

    private BigDecimal balance;

    private String partyType;

    private Long partyId;

    private String partyName;

    private String businessType;

    private LocalDateTime occurDate;

    private Integer paymentMethod;

    private String bankAccount;

    private String bankName;

    private String transactionNo;

    private String remark;

    /** 对账标记：0-未对账 1-已对账（在线支付对账单） */
    private Integer reconcileFlag;

    /** 对账人 */
    private String reconcileBy;

    /** 对账时间 */
    private LocalDateTime reconcileAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
