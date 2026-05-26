package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

/**
 * 报表实例查询请求
 */
@Data
@Schema(description = "报表实例查询请求")
public class ReportInstanceQueryRequest {

    @Schema(description = "报表类型")
    private String reportType;

    @Schema(description = "报表名称")
    private String reportName;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "状态")
    private String status; // DRAFT, GENERATING, COMPLETED, FAILED, PUBLISHED, ARCHIVED

    @Schema(description = "生成人")
    private String generatedBy;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
