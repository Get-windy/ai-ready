package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务报表实体
 */
@Data
@TableName("fin_financial_report")
public class FinancialReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportNo; // 报表编号
    private String reportType; // 报表类型 (BALANCE_SHEET-资产负债表, PROFIT_STATEMENT-利润表, CASH_FLOW-现金流量表, OTHER-其他)
    private String reportName; // 报表名称
    private String reportTitle; // 报表标题
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    private String currency; // 货币单位
    
    private String reportContent; // 报表内容 (JSON格式存储具体数据)
    private String reportSummary; // 报表摘要
    private String reportAnalysis; // 报表分析
    
    private String status; // 状态 (DRAFT-草稿, SUBMITTED-已提交, AUDITED-已审核, APPROVED-已批准, PUBLISHED-已发布)
    private String version; // 版本号
    private String creator; // 创建人
    private String auditor; // 审核人
    private String approver; // 批准人
    
    private BigDecimal totalAmount; // 总金额
    private String amountUnit; // 金额单位 (元, 万元, 千万元, 亿元)
    
    private String dataSource; // 数据来源
    private String generator; // 生成方式 (MANUAL-手工录入, AUTO-自动生成, IMPORT-导入)
    
    private String tags; // 标签 (JSON格式)
    private String permissions; // 权限配置 (JSON格式)
    
    private String remarks; // 备注
    private String attachments; // 附件信息 (JSON格式)
    
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
