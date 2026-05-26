package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 资产负债表查询请求
 */
@Data
@Schema(description = "资产负债表查询请求")
public class BalanceSheetQueryRequest {

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "报表编号")
    private String reportNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
