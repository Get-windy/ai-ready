package cn.aiedge.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 财务报表响应DTO
 */
@Data
public class FinancialReportVO {

    private Long id;
    private Long tenantId;
    private String reportNo; // 报表编号
    private String reportType; // 报表类型
    private String reportName; // 报表名称
    private String reportTitle; // 报表标题
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    private String currency; // 货币单位
    
    private String reportContent; // 报表内容
    private String reportSummary; // 报表摘要
    private String reportAnalysis; // 报表分析
    
    private String status; // 状态
    private String version; // 版本号
    private String creator; // 创建人
    private String auditor; // 审核人
    private String approver; // 批准人
    
    private BigDecimal totalAmount; // 总金额
    private String amountUnit; // 金额单位
    
    private String dataSource; // 数据来源
    private String generator; // 生成方式
    
    private String tags; // 标签
    private String permissions; // 权限配置
    
    private String remarks; // 备注
    private String attachments; // 附件信息
    
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
