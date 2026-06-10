package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 付款记录查询请求
 */
@Data
@Schema(description = "付款记录查询请求")
public class FeePaymentQueryRequest {

    @Schema(description = "业务类型: APPLICATION/REIMBURSEMENT")
    private String businessType;

    @Schema(description = "业务单据ID")
    private Long businessId;

    @Schema(description = "付款单号")
    private String paymentNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "付款日期起")
    private LocalDate paymentDateStart;

    @Schema(description = "付款日期止")
    private LocalDate paymentDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}
