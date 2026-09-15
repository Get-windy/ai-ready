package cn.aiedge.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日对账详情 VO（GET /api/reconciliation/{id}）
 *
 * <p>字段与 {@link cn.aiedge.payment.entity.PaymentReconciliation} 一一对应，
 * 额外携带差异明细子表 {@link #diffRecords}（前端「对账详情」抽屉的「差异明细」表格使用）。</p>
 */
@Data
public class ReconciliationDetailVO {

    private Long id;

    private Long tenantId;

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

    /** 差异处理方式: MANUAL手工调账, IGNORE忽略差异, REPROCESS重新对账 */
    private String handleMethod;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 差异明细
     *
     * <p>由 {@code ReconciliationServiceImpl#getReconciliationDetail} 按本记录的「对账日期 + 渠道」
     * <b>实时比对</b>得出，无需新增子表：</p>
     * <ul>
     *   <li>我方 = {@code payment_request}（`status=2` 已支付，`create_time` 落在对账日）；</li>
     *   <li>渠道侧 = {@code payment_record}（`status=2` 成功回执，经 `request_id` 关联我方请求）。</li>
     * </ul>
     *
     * <p>三类差异（全部由真实数据比对得出，不做任何伪造）：<b>渠道缺单</b>（我方已支付但渠道无成功回执）、
     * <b>金额不一致</b>（双方均成功但金额不等）、<b>平台缺单</b>（渠道当日成功回执但我方无对应已支付请求）。
     * 差异类型与判定条件属本实现定义的口径（对标系统未给出明细字段口径），详见实现类 javadoc。</p>
     *
     * <p>汇总列 {@code diffAmount} 取各明细 {@code |amount|} 之和（差异规模，避免正负相抵掩盖差异）。</p>
     */
    private List<DiffRecord> diffRecords = new ArrayList<>();

    /** 差异明细行（字段与前端 diffColumns 对齐：type/ourAmount/channelAmount/amount/remark） */
    @Data
    public static class DiffRecord {

        /** 业务单号（支付请求号 / 渠道流水号） */
        private String bizNo;

        /** 差异类型（如 金额不一致 / 渠道无单 / 平台无单） */
        private String type;

        /** 我方金额 */
        private BigDecimal ourAmount;

        /** 渠道金额 */
        private BigDecimal channelAmount;

        /** 差异金额（渠道金额 - 我方金额） */
        private BigDecimal amount;

        /** 说明 */
        private String remark;
    }
}
