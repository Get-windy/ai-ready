package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款记录查询请求
 */
@Data
@Schema(description = "付款记录查询请求")
public class PaymentQueryRequest {

    @Schema(description = "应付账款ID")
    private Long payableId;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "付款日期起")
    private LocalDate paymentDateStart;

    @Schema(description = "付款日期止")
    private LocalDate paymentDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
