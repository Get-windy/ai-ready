package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 图表配置创建请求
 */
@Data
@Schema(description = "图表配置创建请求")
public class ChartConfigCreateRequest {

    @Schema(description = "图表名称", required = true)
    @NotBlank(message = "图表名称不能为空")
    private String chartName;

    @Schema(description = "图表类型", required = true)
    @NotBlank(message = "图表类型不能为空")
    private String chartType; // BAR-柱状图, LINE-折线图, PIE-饼图, AREA-面积图, SCATTER-散点图, TABLE-表格, DASHBOARD-仪表板

    @Schema(description = "图表子类型")
    private String chartSubType;

    @Schema(description = "报表模板ID", required = true)
    private Long reportTemplateId;

    @Schema(description = "数据源ID")
    private String dataSourceId;

    @Schema(description = "查询语句")
    private String sqlQuery;

    @Schema(description = "X轴字段")
    private String xAxisField;

    @Schema(description = "Y轴字段")
    private String yAxisField;

    @Schema(description = "系列字段")
    private String seriesField;

    @Schema(description = "颜色方案")
    private String colorScheme;

    @Schema(description = "图例位置")
    private String legendPosition; // TOP, BOTTOM, LEFT, RIGHT

    @Schema(description = "图表选项")
    private String chartOptions; // JSON格式

    @Schema(description = "数据过滤器")
    private String dataFilters; // JSON格式

    @Schema(description = "权限配置")
    private String permissions; // JSON格式

    @Schema(description = "是否堆叠")
    private Boolean isStacked;

    @Schema(description = "是否百分比")
    private Boolean isPercentage;

    @Schema(description = "是否显示网格")
    private Boolean showGrid;

    @Schema(description = "是否显示图例")
    private Boolean showLegend;

    @Schema(description = "是否显示标签")
    private Boolean showLabels;

    @Schema(description = "是否显示动画")
    private Boolean showAnimation;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "排序")
    private Integer sortOrder;
}
