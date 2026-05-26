package cn.aiedge.finance.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 财务报表查询请求DTO
 */
@Data
public class FinancialReportQueryRequest {

    private Integer pageNum = 1; // 页码
    private Integer pageSize = 10; // 每页大小
    
    private String reportType; // 报表类型
    private String reportNo; // 报表编号
    private String reportName; // 报表名称
    private LocalDate reportDate; // 报表日期
    private LocalDate startDate; // 开始日期
    private LocalDate endDate; // 结束日期
    private String status; // 状态
    private String creator; // 创建人
    private String period; // 期间
    private String tenantId; // 租户ID
    
    // 排序字段
    private String orderBy = "createTime";
    private String orderDirection = "DESC"; // ASC, DESC
}
