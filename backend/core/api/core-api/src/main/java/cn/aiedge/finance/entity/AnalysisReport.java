package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分析报告实体
 */
@Data
@TableName("fin_analysis_report")
public class AnalysisReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportCode; // 报告编码
    private String reportName; // 报告名称
    private String reportType; // 报告类型 (DASHBOARD-仪表板, SUMMARY-汇总报告, DETAILED-详细报告, COMPARISON-对比报告)
    private String reportCategory; // 报告分类 (FINANCIAL-财务报告, OPERATIONAL-运营报告, SALES-销售报告, CUSTOMER-客户报告)
    
    private LocalDate reportDate; // 报告日期
    private String period; // 报告期间
    private String reportPeriodType; // 报告期间类型 (DAILY-日, WEEKLY-周, MONTHLY-月, QUARTERLY-季度, YEARLY-年)
    
    private String dataSource; // 数据来源
    private String analysisIds; // 包含的分析ID列表 (逗号分隔)
    private String reportContent; // 报告内容 (HTML/Markdown格式)
    private String executiveSummary; // 执行摘要
    private String conclusions; // 结论
    private String recommendations; // 建议
    
    private String status; // 状态 (DRAFT-草稿, REVIEWING-审核中, APPROVED-已批准, PUBLISHED-已发布, ARCHIVED-已归档)
    private String visibility; // 可见性 (PUBLIC-公开, PRIVATE-私有, TEAM-团队, DEPARTMENT-部门)
    
    private String author; // 作者
    private String reviewer; // 审核人
    private String approver; // 批准人
    private String publisher; // 发布人
    
    private String reportFormat; // 报告格式 (PDF, EXCEL, WORD, HTML, POWERPOINT)
    private String reportTemplate; // 报告模板
    private String chartConfiguration; // 图表配置 (JSON格式)
    private String filterConfiguration; // 过滤器配置 (JSON格式)
    
    private String tags; // 标签 (逗号分隔)
    private String keywords; // 关键词 (逗号分隔)
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
