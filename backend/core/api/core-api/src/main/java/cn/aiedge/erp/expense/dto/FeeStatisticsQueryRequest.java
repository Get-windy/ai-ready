package cn.aiedge.erp.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 费用统计查询请求
 */
@Data
@Schema(description = "费用统计查询请求")
public class FeeStatisticsQueryRequest {

    @Schema(description = "统计年份")
    private Integer statYear;

    @Schema(description = "统计月份")
    private Integer statMonth;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "费用类型")
    private String expenseType;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}
