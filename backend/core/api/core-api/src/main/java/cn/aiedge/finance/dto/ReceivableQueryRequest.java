package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款查询请求
 */
@Data
@Schema(description = "应收账款查询请求")
public class ReceivableQueryRequest {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "开票日期起")
    private LocalDate billDateStart;

    @Schema(description = "开票日期止")
    private LocalDate billDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
