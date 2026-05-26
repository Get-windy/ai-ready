package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 报表模板实体
 */
@Data
@TableName("fin_report_template")
public class ReportTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String templateCode; // 模板编码
    private String templateName; // 模板名称
    private String templateType; // 模板类型 (FINANCIAL-财务报表, SALES-销售报表, INVENTORY-库存报表, CUSTOM-自定义)
    private String category; // 模板分类
    private String description; // 模板描述
    
    private String templateConfig; // 模板配置 (JSON格式)
    private String fieldMappings; // 字段映射 (JSON格式)
    private String dataFilters; // 数据过滤器 (JSON格式)
    private String permissions; // 权限配置 (JSON格式)
    
    private String sqlQuery; // SQL查询语句
    private String dataSourceId; // 数据源ID
    private String chartConfigs; // 图表配置 (JSON格式)
    
    private Boolean isDefault; // 是否默认模板
    private Boolean isActive; // 是否激活
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
