package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账款交账 - 按职员视图行 VO
 * 数据源：按交账职员分组聚合销售出库/退货单
 * 对齐对标 6 列 + 派生分组「待交账收款金额（使用预收/收款合计）」
 */
@Data
@Schema(description = "账款交账 按职员视图行VO")
public class AccountDeliveryStaffVO {

    @Schema(description = "交账职员（业务员/司机）")
    private String deliverStaff;

    @Schema(description = "部门")
    private String departmentName;

    @Schema(description = "待交账单据（张数）")
    private Long docCount;

    @Schema(description = "待交账金额")
    private BigDecimal totalAmount;

    @Schema(description = "使用预收")
    private BigDecimal usedAdvance;

    @Schema(description = "收款合计")
    private BigDecimal receiveTotal;

    @Schema(description = "待交账优惠")
    private BigDecimal favorableAmount;

    @Schema(description = "待交账欠款金额")
    private BigDecimal arrearsAmount;
}
