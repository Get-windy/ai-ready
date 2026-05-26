package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.BalanceSheet;
import cn.aiedge.finance.entity.ProfitStatement;
import cn.aiedge.finance.entity.CashFlowStatement;
import cn.aiedge.finance.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 财务报表服务接口
 */
public interface IFinancialStatementService {

    // 资产负债表相关方法
    /**
     * 生成资产负债表
     */
    Long generateBalanceSheet(String period, Long tenantId);

    /**
     * 生成资产负债表 - 特定方法用于财务报表模块
     */
    Long generateBalanceSheetReport(String period, Long tenantId);

    /**
     * 分页查询资产负债表
     */
    Page<BalanceSheetVO> pageBalanceSheets(BalanceSheetQueryRequest request);

    /**
     * 根据ID获取资产负债表详情
     */
    BalanceSheetVO getBalanceSheetById(Long id);

    /**
     * 删除资产负债表
     */
    void deleteBalanceSheet(Long id);

    // 利润表相关方法
    /**
     * 生成利润表
     */
    Long generateProfitStatement(String period, Long tenantId);

    /**
     * 分页查询利润表
     */
    Page<ProfitStatementVO> pageProfitStatements(ProfitStatementQueryRequest request);

    /**
     * 根据ID获取利润表详情
     */
    ProfitStatementVO getProfitStatementById(Long id);

    /**
     * 删除利润表
     */
    void deleteProfitStatement(Long id);

    // 现金流量表相关方法
    /**
     * 生成现金流量表
     */
    Long generateCashFlowStatement(String period, Long tenantId);

    /**
     * 分页查询现金流量表
     */
    Page<CashFlowStatementVO> pageCashFlowStatements(CashFlowStatementQueryRequest request);

    /**
     * 根据ID获取现金流量表详情
     */
    CashFlowStatementVO getCashFlowStatementById(Long id);

    /**
     * 删除现金流量表
     */
    void deleteCashFlowStatement(Long id);

    /**
     * 审核报表
     */
    void auditReport(String reportType, Long id, String auditor);

    /**
     * 批量生成报表
     */
    void batchGenerateReports(String period, Long tenantId);
}
