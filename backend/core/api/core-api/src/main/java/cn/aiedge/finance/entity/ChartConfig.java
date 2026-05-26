package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 图表配置实体
 */
@Data
@TableName("fin_chart_config")
public class ChartConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String chartCode; // 图表编码
    private String chartName; // 图表名称
    private String chartType; // 图表类型 (BAR-柱状图, LINE-折线图, PIE-饼图, AREA-面积图, SCATTER-散点图, TABLE-表格, DASHBOARD-仪表板)
    private String chartSubType; // 图表子类型
    
    private Long reportTemplateId; // 关联的报表模板ID
    private String dataSourceId; // 数据源ID
    private String sqlQuery; // 查询语句
    
    private String xAxisField; // X轴字段
    private String yAxisField; // Y轴字段
    private String seriesField; // 系列字段
    private String colorScheme; // 颜色方案
    private String legendPosition; // 图例位置 (TOP, BOTTOM, LEFT, RIGHT)
    
    private String chartOptions; // 图表选项 (JSON格式)
    private String dataFilters; // 数据过滤器 (JSON格式)
    private String permissions; // 权限配置 (JSON格式)
    
    private Boolean isStacked; // 是否堆叠
    private Boolean isPercentage; // 是否百分比
    private Boolean showGrid; // 是否显示网格
    private Boolean showLegend; // 是否显示图例
    private Boolean showLabels; // 是否显示标签
    private Boolean showAnimation; // 是否显示动画
    
    private Integer width; // 宽度
    private Integer height; // 高度
    private Integer sortOrder; // 排序
    
    private String createdBy; // 创建人
    private String updatedBy; // 更新人
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
