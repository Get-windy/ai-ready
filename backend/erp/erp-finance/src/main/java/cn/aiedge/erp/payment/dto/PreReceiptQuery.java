package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 预收款单列表多条件查询
 */
@Data
@Schema(description = "预收款单列表查询")
public class PreReceiptQuery {

    @Schema(description = "单据编号")
    private String preReceiptNo;

    @Schema(description = "结算单位")
    private String customerName;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "经手人")
    private String handlerName;

    @Schema(description = "部门")
    private String deptName;

    @Schema(description = "制单人")
    private String creatorName;

    @Schema(description = "记账人")
    private String bookkeeperName;

    @Schema(description = "审核人")
    private String auditorName;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "收款账户")
    private String bankAccount;

    @Schema(description = "单据备注")
    private String remark;

    @Schema(description = "开始日期")
    private LocalDate dateStart;

    @Schema(description = "结束日期")
    private LocalDate dateEnd;
}
