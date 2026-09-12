package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账款交账 查询参数 DTO
 * 数据源：销售出库单（配送代收/业务员代收）+ 销售退货单
 * 对标：财务→收入支出→账款交账（按职员 / 按单据 双视图）
 */
@Data
@Schema(description = "账款交账查询参数DTO")
public class AccountDeliveryQueryDTO {

    // ═══ 分页 ═══
    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    // ═══ 公共：业务日期范围（默认今日） ═══
    @Schema(description = "开始日期 yyyy-MM-dd")
    private String dateStart;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private String dateEnd;

    // ═══ 按职员视图 ═══
    @Schema(description = "交账职员（业务员/司机）")
    private String staffName;

    // ═══ 按单据视图查询条件（对标 18 字段） ═══
    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "结算单位")
    private String settlementUnit;

    @Schema(description = "收款账户")
    private String receiptAccount;

    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "配送任务编号")
    private String deliveryTaskNo;

    @Schema(description = "单据备注")
    private String remark;

    @Schema(description = "账款确认（是/否）")
    private String confirmFlag;

    @Schema(description = "账款类型（全部/送货代收/上门收款等）")
    private String deliveryType;

    @Schema(description = "交账状态（未交账/已交账）")
    private String deliverStatus;

    @Schema(description = "业务类型（销售出库单/销售退货单）")
    private String businessType;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "单据类型")
    private String documentType;

    @Schema(description = "客户结款方式")
    private String settlementMethod;

    @Schema(description = "收款单状态")
    private String receiptStatus;

    @Schema(description = "收款单编号")
    private String receiptNo;

    @Schema(description = "在线支付流水号")
    private String onlinePayNo;

    @Schema(description = "业务经手人")
    private String handlerName;

    @Schema(description = "确认交账人")
    private String confirmStaff;

    @Schema(description = "确认交账时间起始")
    private String confirmStartDate;

    @Schema(description = "确认交账时间结束")
    private String confirmEndDate;

    @Schema(description = "显示全部待交账单据（true 时忽略交账状态过滤，默认 true）")
    private Boolean showAllPending = true;

    // ═══ 交账动作入参 ═══
    @Schema(description = "交账动作：单据类型（OUTBOUND/RETURN）")
    private String sourceType;

    @Schema(description = "交账动作：单据ID集合")
    private java.util.List<Long> sourceIds;

    @Schema(description = "交账动作：按职员交账时的职员名")
    private String deliverStaff;

    // ═══ 金额范围（备用） ═══
    @Schema(description = "最小待交账金额")
    private BigDecimal minAmount;

    @Schema(description = "最大待交账金额")
    private BigDecimal maxAmount;
}
