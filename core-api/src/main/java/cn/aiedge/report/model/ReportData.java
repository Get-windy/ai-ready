package cn.aiedge.report.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表数据
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "报表数据")
public class ReportData implements Serializable {

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
     * 生成时间
     */
    @Schema(description = "生成时间")
    private LocalDateTime generatedAt;

    /**
     * 参数值
     */
    @Schema(description = "参数值")
    private Map<String, Object> parameters;

    /**
     * 列定义
     */
    @Schema(description = "列定义")
    private List<ReportDefinition.ReportColumn> columns;

    /**
     * 数据行
     */
    @Schema(description = "数据行")
    private List<Map<String, Object>> rows;

    /**
     * 总行数
     */
    @Schema(description = "总行数")
    private long totalRows;

    /**
     * 汇总数据
     */
    @Schema(description = "汇总数据")
    private Map<String, Object> summary;

    /**
     * 图表数据（报表类型为chart时）
     */
    @Schema(description = "图表数据")
    private ChartData chartData;

    /**
     * 查询耗时（毫秒）
     */
    @Schema(description = "查询耗时（毫秒）")
    private long queryTime;

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    public List<ReportDefinition.ReportColumn> getColumns() { return columns; }
    public void setColumns(List<ReportDefinition.ReportColumn> columns) { this.columns = columns; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
    public ChartData getChartData() { return chartData; }
    public void setChartData(ChartData chartData) { this.chartData = chartData; }
    public long getQueryTime() { return queryTime; }
    public void setQueryTime(long queryTime) { this.queryTime = queryTime; }

    /**
     * 图表数据
     */
    @Schema(description = "图表数据")
    public static class ChartData implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "标签列表")
        private List<String> labels;
        
        @Schema(description = "数据集列表")
        private List<ChartDataset> datasets;
        
        @Schema(description = "图表配置")
        private Map<String, Object> options;

        // Getters and Setters
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<ChartDataset> getDatasets() { return datasets; }
        public void setDatasets(List<ChartDataset> datasets) { this.datasets = datasets; }
        public Map<String, Object> getOptions() { return options; }
        public void setOptions(Map<String, Object> options) { this.options = options; }
    }

    /**
     * 图表数据集
     */
    @Schema(description = "图表数据集")
    public static class ChartDataset implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        @Schema(description = "数据集标签")
        private String label;
        
        @Schema(description = "数据值")
        private List<Double> data;
        
        @Schema(description = "背景色")
        private List<String> backgroundColor;
        
        @Schema(description = "边框色")
        private String borderColor;
        
        @Schema(description = "填充")
        private boolean fill;

        // Getters and Setters
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<Double> getData() { return data; }
        public void setData(List<Double> data) { this.data = data; }
        public List<String> getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(List<String> backgroundColor) { this.backgroundColor = backgroundColor; }
        public String getBorderColor() { return borderColor; }
        public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
        public boolean isFill() { return fill; }
        public void setFill(boolean fill) { this.fill = fill; }
    }
}
