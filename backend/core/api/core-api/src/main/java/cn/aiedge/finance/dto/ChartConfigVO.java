package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 图表配置VO
 */
@Data
@Schema(description = "图表配置详情")
public class ChartConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "图表编码")
    private String chartCode;

    @Schema(description = "图表名称")
    private String chartName;

    @Schema(description = "图表类型")
    private String chartType; // BAR-柱状图, LINE-折线图, PIE-饼图, AREA-面积图, SCATTER-散点图, TABLE-表格, DASHBOARD-仪表板

    @Schema(description = "图表子类型")
    private String chartSubType;

    @Schema(description = "报表模板ID")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
