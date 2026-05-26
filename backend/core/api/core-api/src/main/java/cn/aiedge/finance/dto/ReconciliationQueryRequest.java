package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 对账记录查询请求
 */
@Data
@Schema(description = "对账记录查询请求")
public class ReconciliationQueryRequest {

    @Schema(description = "对账类型")
    private String reconciliationType; // BANK-银行 CUSTOMER-客户 SUPPLIER-供应商

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "状态")
    private Integer status; // 0-待对账 1-已对账 2-有差异

    @Schema(description = "开始日期起")
    private LocalDate startDateStart;

    @Schema(description = "开始日期止")
    private LocalDate startDateEnd;

    @Schema(description = "结束日期起")
    private LocalDate endDateStart;

    @Schema(description = "结束日期止")
    private LocalDate endDateEnd;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
