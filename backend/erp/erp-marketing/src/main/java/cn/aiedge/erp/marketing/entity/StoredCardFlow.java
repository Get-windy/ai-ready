package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 储值卡收支流水（含变动后余额，可对账） */
@Data
@Accessors(chain = true)
@TableName("mkt_stored_card_flow")
public class StoredCardFlow {

    public static final String ISSUE = "ISSUE";
    public static final String RECHARGE = "RECHARGE";
    public static final String BONUS = "BONUS";
    public static final String CONSUME = "CONSUME";
    public static final String REFUND = "REFUND";
    public static final String ADJUST = "ADJUST";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private Long cardId;
    private String cardNo;
    private Long partnerId;

    /** ISSUE / RECHARGE / BONUS / CONSUME / REFUND / ADJUST */
    private String flowType;
    /** 本金变动（正=增加，负=减少） */
    private BigDecimal amount;
    /** 赠送金额变动（正=增加） */
    private BigDecimal bonusAmount;
    /** 变动后余额 */
    private BigDecimal balanceAfter;
    private String sourceBillNo;
    private String handlerName;
    private String remark;
    /** 已生成的会计凭证号（幂等标记：非空即不再重复生成） */
    private String voucherNo;
    /** 结算账户：CASH 库存现金 / BANK 银行存款 */
    private String settleAccount;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
