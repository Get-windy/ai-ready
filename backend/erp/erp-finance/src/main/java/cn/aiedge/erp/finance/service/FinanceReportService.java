package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.FinanceReport;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务报表Service接口
 */
public interface FinanceReportService {
    
    /**
     * 生成财务报表
     */
    boolean generateReport(Integer reportType, String reportPeriod);
    
    /**
     * 查询报表列表
     */
    List<FinanceReport> listReports(Integer reportType, String reportPeriod, Integer status);
    
    /**
     * 查询报表详情
     */
    FinanceReport getReportDetail(String reportNo);
    
    /**
     * 审核报表
     */
    boolean approveReport(String reportNo, String approvedBy);
    
    /**
     * 删除草稿报表
     */
    boolean deleteDraftReport(String reportNo);
}
