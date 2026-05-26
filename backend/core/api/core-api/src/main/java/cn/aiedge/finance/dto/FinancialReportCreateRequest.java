package cn.aiedge.finance.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 财务报表创建请求DTO
 */
@Data
public class FinancialReportCreateRequest {

    private String reportType; // 报表类型
    private String reportName; // 报表名称
    private String reportTitle; // 报表标题
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    private String currency; // 货币单位
    private String reportContent; // 报表内容
    private String reportSummary; // 报表摘要
    private String reportAnalysis; // 报表分析
    private String dataSource; // 数据来源
    private String generator; // 生成方式 (AUTO, MANUAL, IMPORT)
    
    private String tags; // 标签
    private String permissions; // 权限配置
    private String remarks; // 备注
    private String attachments; // 附件信息
}
