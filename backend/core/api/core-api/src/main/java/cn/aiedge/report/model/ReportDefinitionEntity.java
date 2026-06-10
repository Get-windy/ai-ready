package cn.aiedge.report.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自定义报表定义实体
 * <p>
 * 对应数据库表 report_definition，用于持久化用户自定义报表。
 * 完整的报表定义（含列定义、参数、图表配置等）以JSON格式存储在 definition_json 字段中。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("report_definition")
public class ReportDefinitionEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报表ID（业务唯一标识）
     */
    private String reportId;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表编码
     */
    private String reportCode;

    /**
     * 报表类型: summary/detail/chart/pivot
     */
    private String reportType;

    /**
     * 报表分类
     */
    private String category;

    /**
     * 数据源类型: sql/api/custom
     */
    private String dataSourceType;

    /**
     * 报表定义完整JSON（含 ReportDefinition 的所有字段）
     */
    private String definitionJson;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleted;
}
