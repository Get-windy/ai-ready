package cn.aiedge.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "报表定义")
public class ReportDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报表ID
     */
    @Schema(description = "报表ID")
    private String reportId;

    /**
     * 报表名称
     */
    @Schema(description = "报表名称")
    private String reportName;

    /**
     * 报表编码
     */
    @Schema(description = "报表编码")
    private String reportCode;

    /**
     * 报表类型: summary/detail/chart/pivot
     */
    @Schema(description = "报表类型: summary/detail/chart/pivot")
    private String reportType;

    /**
     * 报表分类
     */
    @Schema(description = "报表分类")
    private String category;

    /**
     * 数据源类型: sql/api/custom
     */
    @Schema(description = "数据源类型: sql/api/custom")
    private String dataSourceType;

    /**
     * 数据源配置
     */
    @Schema(description = "数据源配置")
    private Map<String, Object> dataSourceConfig;

    /**
     * 列定义
     */
    @Schema(description = "列定义")
    private List<ReportColumn> columns;

    /**
     * 参数定义
     */
    @Schema(description = "参数定义")
    private List<ReportParameter> parameters;

    /**
     * 图表配置（报表类型为chart时）
     */
    @Schema(description = "图表配置")
    private ChartConfig chartConfig;

    /**
     * 是否启用
     */
    @Schema(description = "是否启用")
    private boolean enabled;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private int sortOrder;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDataSourceType() { return dataSourceType; }
    public void setDataSourceType(String dataSourceType) { this.dataSourceType = dataSourceType; }
    public Map<String, Object> getDataSourceConfig() { return dataSourceConfig; }
    public void setDataSourceConfig(Map<String, Object> dataSourceConfig) { this.dataSourceConfig = dataSourceConfig; }
    public List<ReportColumn> getColumns() { return columns; }
    public void setColumns(List<ReportColumn> columns) { this.columns = columns; }
    public List<ReportParameter> getParameters() { return parameters; }
    public void setParameters(List<ReportParameter> parameters) { this.parameters = parameters; }
    public ChartConfig getChartConfig() { return chartConfig; }
    public void setChartConfig(ChartConfig chartConfig) { this.chartConfig = chartConfig; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /**
     * 报表列定义
     */
    @Schema(description = "报表列定义")
    public static class ReportColumn implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "字段名")
        private String field;
        
        @Schema(description = "列标题")
        private String title;
        
        @Schema(description = "数据类型: string/number/date/datetime/boolean")
        private String dataType;
        
        @Schema(description = "列宽")
        private int width;
        
        @Schema(description = "格式化模式")
        private String format;
        
        @Schema(description = "是否聚合")
        private boolean aggregate;
        
        @Schema(description = "聚合方式: sum/avg/count/max/min")
        private String aggregateType;
        
        @Schema(description = "是否排序")
        private boolean sortable;
        
        @Schema(description = "是否可见")
        private boolean visible;

        // Getters and Setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public boolean isAggregate() { return aggregate; }
        public void setAggregate(boolean aggregate) { this.aggregate = aggregate; }
        public String getAggregateType() { return aggregateType; }
        public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
        public boolean isSortable() { return sortable; }
        public void setSortable(boolean sortable) { this.sortable = sortable; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
    }

    /**
     * 报表参数定义
     */
    @Schema(description = "报表参数定义")
    public static class ReportParameter implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "参数名")
        private String name;
        
        @Schema(description = "参数标题")
        private String title;
        
        @Schema(description = "数据类型: string/number/date/select/multiSelect")
        private String dataType;
        
        @Schema(description = "是否必填")
        private boolean required;
        
        @Schema(description = "默认值")
        private Object defaultValue;
        
        @Schema(description = "可选值（select/multiSelect类型）")
        private List<OptionItem> options;
        
        @Schema(description = "验证规则")
        private String validation;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        public List<OptionItem> getOptions() { return options; }
        public void setOptions(List<OptionItem> options) { this.options = options; }
        public String getValidation() { return validation; }
        public void setValidation(String validation) { this.validation = validation; }
    }

    /**
     * 可选项
     */
    @Schema(description = "可选项")
    public static class OptionItem implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "值")
        private String value;
        
        @Schema(description = "标签")
        private String label;

        public OptionItem() {}
        
        public OptionItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        // Getters and Setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    /**
     * 图表配置
     */
    @Schema(description = "图表配置")
    public static class ChartConfig implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "图表类型: line/bar/pie/doughnut/radar/polarArea")
        private String chartType;
        
        @Schema(description = "X轴字段")
        private String xField;
        
        @Schema(description = "Y轴字段列表")
        private List<String> yFields;
        
        @Schema(description = "分组字段")
        private String groupField;
        
        @Schema(description = "是否堆叠")
        private boolean stacked;
        
        @Schema(description = "是否显示图例")
        private boolean showLegend;
        
        @Schema(description = "图表标题")
        private String chartTitle;

        // Getters and Setters
        public String getChartType() { return chartType; }
        public void setChartType(String chartType) { this.chartType = chartType; }
        public String getxField() { return xField; }
        public void setxField(String xField) { this.xField = xField; }
        public List<String> getyFields() { return yFields; }
        public void setyFields(List<String> yFields) { this.yFields = yFields; }
        public String getGroupField() { return groupField; }
        public void setGroupField(String groupField) { this.groupField = groupField; }
        public boolean isStacked() { return stacked; }
        public void setStacked(boolean stacked) { this.stacked = stacked; }
        public boolean isShowLegend() { return showLegend; }
        public void setShowLegend(boolean showLegend) { this.showLegend = showLegend; }
        public String getChartTitle() { return chartTitle; }
        public void setChartTitle(String chartTitle) { this.chartTitle = chartTitle; }
    }
}
