package cn.aiedge.dms.payment.dto;

import cn.aiedge.dms.payment.entity.DmsPayment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款台账行（收款记录 + 联查的任务/客户/配送员快照 + 挂账催收/交款时限派生列）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DmsPaymentVO extends DmsPayment {

    /** 任务编号 */
    private String taskNo;

    /** 客户名称 */
    private String customerName;

    /** 客户电话 */
    private String customerPhone;

    /** 配送员姓名 */
    private String riderName;

    /** 任务代收货款（应代数） */
    private BigDecimal taskCollectOnDelivery;

    /** 任务配送费 */
    private BigDecimal taskDeliveryFee;

    // ── 挂账催收（《收款管理开发文档》§3.1 未付管理）──

    /** 累计催收次数 */
    private Integer urgeCount;

    /** 最近催收时间 */
    private LocalDateTime lastUrgeTime;

    /** 客户承诺付款日（最近一次） */
    private LocalDate promiseDate;

    /** 累计核销金额（挂账收回） */
    private BigDecimal writeOffAmount;

    // ── 交款时限（§3.4-2 资金安全：超时预警）──

    /** 是否超交款时限（已支付未交清 且 超过 `dms.payment.handover.deadline.hours`） */
    private Boolean overdue;

    /** 已超时小时数（未超时为 0） */
    private Long overdueHours;
}
