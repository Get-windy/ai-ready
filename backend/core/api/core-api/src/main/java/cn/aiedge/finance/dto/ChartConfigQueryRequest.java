package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图表配置查询请求
 */
@Data
@Schema(description = "图表配置查询请求")
public class ChartConfigQueryRequest {

    @Schema(description = "图表类型")
    private String chartType; // BAR-柱状图, LINE-折线图, PIE-饼图, AREA-面积图, SCATTER-散点图, TABLE-表格, DASHBOARD-仪表板

    @Schema(description = "图表名称")
    private String chartName;

    @Schema(description = "报表模板ID")
    private Long reportTemplateId;

    @Schema(description = "数据源ID")
    private String dataSourceId;

    @Schema(description = "是否显示网格")
    private Boolean showGrid;

    @Schema(description = "是否显示图例")
    private Boolean showLegend;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
