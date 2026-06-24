package cn.aiedge.payment.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日对账记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_reconciliation")
public class PaymentReconciliation extends BaseEntity {

    /** 对账日期 */
    private LocalDate reconcileDate;

    /** 支付渠道 */
    private String channel;

    /** 总笔数 */
    private Integer totalCount;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 成功笔数 */
    private Integer successCount;

    /** 成功金额 */
    private BigDecimal successAmount;

    /** 差异笔数 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 对账状态: 0待对账, 1已对账, 2有差异 */
    private Integer status;

    /** 对账时间 */
    private LocalDate reconciledTime;

    /** 备注 */
    private String remark;
}