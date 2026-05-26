package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.FinancialReport;
import cn.aiedge.finance.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 财务报表服务接口
 */
public interface IFinancialReportService {

    /**
     * 生成财务报表
     */
    Long generateFinancialReport(FinancialReportCreateRequest request, Long tenantId);

    /**
     * 分页查询财务报表
     */
    Page<FinancialReportVO> pageFinancialReports(FinancialReportQueryRequest request);

    /**
     * 根据ID获取财务报表详情
     */
    FinancialReportVO getFinancialReportById(Long id);

    /**
     * 删除财务报表
     */
    void deleteFinancialReport(Long id);

    /**
     * 更新财务报表
     */
    void updateFinancialReport(Long id, FinancialReportCreateRequest request);

    /**
     * 审核财务报表
     */
    void auditFinancialReport(Long id, String auditor);

    /**
     * 批准财务报表
     */
    void approveFinancialReport(Long id, String approver);

    /**
     * 生成资产负债表
     */
    Long generateBalanceSheetReport(FinancialReportCreateRequest request, Long tenantId);

    /**
     * 生成利润表
     */
    Long generateProfitStatementReport(FinancialReportCreateRequest request, Long tenantId);

    /**
     * 生成现金流量表
     */
    Long generateCashFlowStatementReport(FinancialReportCreateRequest request, Long tenantId);
}
