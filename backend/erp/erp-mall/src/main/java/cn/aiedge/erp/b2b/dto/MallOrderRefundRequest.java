package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商城订单退款请求（POST /api/erp/mall/admin/order/{id}/refund）
 *
 * <p>与前端 {@code mallOrderApi.refund(id, { amount, reason, remark })} 一一对应。</p>
 *
 * <p>口径说明：{@code erp_sale_order} 无独立「退款金额」列，退款金额由前端采集后仅用于业务提示，
 * 后端仅将 {@code payment_status} 置为 4（已退款）——退款率统计亦按 payment_status=4 计数派生。</p>
 */
@Data
public class MallOrderRefundRequest {

    /** 退款金额（无对应库字段，仅记录于备注） */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /** 备注 */
    private String remark;
}
