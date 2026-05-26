package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报表实例实体（生成的报表）
 */
@Data
@TableName("fin_report_instance")
public class ReportInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportCode; // 报表编码
    private String reportName; // 报表名称
    private Long templateId; // 模板ID
    private String reportType; // 报表类型
    
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    private String reportPeriodType; // 报告期间类型 (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)
    
    private String status; // 状态 (DRAFT, GENERATING, COMPLETED, FAILED, PUBLISHED, ARCHIVED)
    private String executionLog; // 执行日志
    private Long executionTime; // 执行耗时（毫秒）
    
    private String reportData; // 报表数据 (JSON格式)
    private String chartData; // 图表数据 (JSON格式)
    private String exportPath; // 导出文件路径
    
    private String filtersApplied; // 应用的过滤器 (JSON格式)
    private String parameters; // 参数 (JSON格式)
    
    private String generatedBy; // 生成人
    private String publishedBy; // 发布人
    private String exportedBy; // 导出人
    
    private String permissions; // 权限配置 (JSON格式)
    private String tags; // 标签
    private String description; // 描述
    
    private Boolean isFavorite; // 是否收藏
    private Boolean isShared; // 是否共享
    private Integer viewCount; // 查看次数
    private Integer downloadCount; // 下载次数
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
