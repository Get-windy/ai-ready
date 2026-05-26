package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.FinancialReport;
import cn.aiedge.finance.entity.BalanceSheet;
import cn.aiedge.finance.entity.ProfitStatement;
import cn.aiedge.finance.entity.CashFlowStatement;
import cn.aiedge.finance.mapper.FinancialReportMapper;
import cn.aiedge.finance.mapper.BalanceSheetMapper;
import cn.aiedge.finance.mapper.ProfitStatementMapper;
import cn.aiedge.finance.mapper.CashFlowStatementMapper;
import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.service.IFinancialReportService;
import cn.aiedge.finance.service.IFinancialStatementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务报表服务实现
 */
@Service
@Transactional
public class FinancialReportServiceImpl implements IFinancialReportService {

    private final FinancialReportMapper financialReportMapper;
    private final BalanceSheetMapper balanceSheetMapper;
    private final ProfitStatementMapper profitStatementMapper;
    private final CashFlowStatementMapper cashFlowStatementMapper;
    private final IFinancialStatementService financialStatementService;

    public FinancialReportServiceImpl(
            FinancialReportMapper financialReportMapper,
            BalanceSheetMapper balanceSheetMapper,
            ProfitStatementMapper profitStatementMapper,
            CashFlowStatementMapper cashFlowStatementMapper,
            IFinancialStatementService financialStatementService) {
        this.financialReportMapper = financialReportMapper;
        this.balanceSheetMapper = balanceSheetMapper;
        this.profitStatementMapper = profitStatementMapper;
        this.cashFlowStatementMapper = cashFlowStatementMapper;
        this.financialStatementService = financialStatementService;
    }

    @Override
    public Long generateFinancialReport(FinancialReportCreateRequest request, Long tenantId) {
        FinancialReport financialReport = new FinancialReport();
        
        // 设置基础信息
        financialReport.setReportNo("FR-" + System.currentTimeMillis());
        financialReport.setReportType(request.getReportType());
        financialReport.setReportName(request.getReportName());
        financialReport.setReportTitle(request.getReportTitle());
        financialReport.setReportDate(request.getReportDate() != null ? request.getReportDate() : LocalDate.now());
        financialReport.setPeriod(request.getPeriod());
        financialReport.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        financialReport.setTenantId(tenantId);
        
        // 设置报表内容
        financialReport.setReportContent(request.getReportContent());
        financialReport.setReportSummary(request.getReportSummary());
        financialReport.setReportAnalysis(request.getReportAnalysis());
        
        // 设置状态和基本信息
        financialReport.setStatus("DRAFT"); // 草稿状态
        financialReport.setVersion("1.0");
        financialReport.setCreator(getCurrentUser());
        financialReport.setAuditor(null);
        financialReport.setApprover(null);
        
        // 设置数据来源和生成方式
        financialReport.setDataSource(request.getDataSource() != null ? request.getDataSource() : "SYSTEM");
        financialReport.setGenerator(request.getGenerator() != null ? request.getGenerator() : "AUTO");
        
        // 设置标签和权限
        financialReport.setTags(request.getTags());
        financialReport.setPermissions(request.getPermissions());
        
        // 设置备注和附件
        financialReport.setRemarks(request.getRemarks());
        financialReport.setAttachments(request.getAttachments());
        
        // 设置创建信息
        financialReport.setCreateBy(getCurrentUser());
        financialReport.setCreateTime(LocalDateTime.now());
        financialReport.setUpdateBy(getCurrentUser());
        financialReport.setUpdateTime(LocalDateTime.now());
        
        financialReportMapper.insert(financialReport);
        return financialReport.getId();
    }

    @Override
    public Page<FinancialReportVO> pageFinancialReports(FinancialReportQueryRequest request) {
        LambdaQueryWrapper<FinancialReport> wrapper = Wrappers.lambdaQuery(FinancialReport.class)
                .eq(request.getReportType() != null, FinancialReport::getReportType, request.getReportType())
                .like(request.getReportNo() != null, FinancialReport::getReportNo, request.getReportNo())
                .like(request.getReportName() != null, FinancialReport::getReportName, request.getReportName())
                .eq(request.getReportDate() != null, FinancialReport::getReportDate, request.getReportDate())
                .between(request.getStartDate() != null && request.getEndDate() != null, 
                         FinancialReport::getReportDate, request.getStartDate(), request.getEndDate())
                .eq(request.getStatus() != null, FinancialReport::getStatus, request.getStatus())
                .like(request.getCreator() != null, FinancialReport::getCreator, request.getCreator())
                .eq(request.getPeriod() != null, FinancialReport::getPeriod, request.getPeriod())
                .orderByDesc(FinancialReport::getCreateTime);

        Page<FinancialReport> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FinancialReport> resultPage = financialReportMapper.selectPage(page, wrapper);

        Page<FinancialReportVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<FinancialReportVO> voList = resultPage.getRecords().stream()
                .map(this::convertFinancialReportToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public FinancialReportVO getFinancialReportById(Long id) {
        FinancialReport financialReport = financialReportMapper.selectById(id);
        if (financialReport != null) {
            return convertFinancialReportToVO(financialReport);
        }
        return null;
    }

    @Override
    public void deleteFinancialReport(Long id) {
        FinancialReport financialReport = financialReportMapper.selectById(id);
        if (financialReport != null) {
            financialReportMapper.deleteById(id);
        }
    }

    @Override
    public void updateFinancialReport(Long id, FinancialReportCreateRequest request) {
        FinancialReport financialReport = financialReportMapper.selectById(id);
        if (financialReport != null) {
            // 更新基本信息
            financialReport.setReportType(request.getReportType());
            financialReport.setReportName(request.getReportName());
            financialReport.setReportTitle(request.getReportTitle());
            financialReport.setReportDate(request.getReportDate());
            financialReport.setPeriod(request.getPeriod());
            financialReport.setCurrency(request.getCurrency());
            
            // 更新报表内容
            financialReport.setReportContent(request.getReportContent());
            financialReport.setReportSummary(request.getReportSummary());
            financialReport.setReportAnalysis(request.getReportAnalysis());
            
            // 更新其他信息
            financialReport.setTags(request.getTags());
            financialReport.setPermissions(request.getPermissions());
            financialReport.setRemarks(request.getRemarks());
            financialReport.setAttachments(request.getAttachments());
            
            // 更新时间信息
            financialReport.setUpdateBy(getCurrentUser());
            financialReport.setUpdateTime(LocalDateTime.now());
            
            financialReportMapper.updateById(financialReport);
        }
    }

    @Override
    public void auditFinancialReport(Long id, String auditor) {
        FinancialReport financialReport = financialReportMapper.selectById(id);
        if (financialReport != null) {
            financialReport.setStatus("AUDITED");
            financialReport.setAuditor(auditor);
            financialReport.setUpdateBy(getCurrentUser());
            financialReport.setUpdateTime(LocalDateTime.now());
            financialReportMapper.updateById(financialReport);
        }
    }

    @Override
    public void approveFinancialReport(Long id, String approver) {
        FinancialReport financialReport = financialReportMapper.selectById(id);
        if (financialReport != null) {
            financialReport.setStatus("APPROVED");
            financialReport.setApprover(approver);
            financialReport.setUpdateBy(getCurrentUser());
            financialReport.setUpdateTime(LocalDateTime.now());
            financialReportMapper.updateById(financialReport);
        }
    }

    @Override
    public Long generateBalanceSheetReport(FinancialReportCreateRequest request, Long tenantId) {
        // 使用现有的资产负债表生成服务来生成报表
        String period = request.getPeriod() != null ? request.getPeriod() : "MONTHLY_" + LocalDate.now().toString().substring(0, 7);
        
        // 调用现有的资产负债表生成服务
        Long balanceSheetId = financialStatementService.generateBalanceSheet(period, tenantId);
        
        // 创建一个对应的财务报告记录
        FinancialReport financialReport = new FinancialReport();
        financialReport.setReportNo("BS-FR-" + System.currentTimeMillis());
        financialReport.setReportType("BALANCE_SHEET");
        financialReport.setReportName(request.getReportName() != null ? request.getReportName() : "资产负债表");
        financialReport.setReportTitle(request.getReportTitle() != null ? request.getReportTitle() : "资产负债表");
        financialReport.setReportDate(request.getReportDate() != null ? request.getReportDate() : LocalDate.now());
        financialReport.setPeriod(period);
        financialReport.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        financialReport.setTenantId(tenantId);
        
        // 设置报表内容（可以是从生成的资产负债表获取的数据）
        financialReport.setReportContent("Generated balance sheet report for period: " + period);
        financialReport.setReportSummary("资产负债表汇总信息");
        financialReport.setReportAnalysis("资产负债表分析");
        
        financialReport.setStatus("DRAFT");
        financialReport.setVersion("1.0");
        financialReport.setCreator(getCurrentUser());
        financialReport.setDataSource("SYSTEM");
        financialReport.setGenerator("AUTO");
        
        financialReport.setCreateBy(getCurrentUser());
        financialReport.setCreateTime(LocalDateTime.now());
        financialReport.setUpdateBy(getCurrentUser());
        financialReport.setUpdateTime(LocalDateTime.now());
        
        financialReportMapper.insert(financialReport);
        return financialReport.getId();
    }

    @Override
    public Long generateProfitStatementReport(FinancialReportCreateRequest request, Long tenantId) {
        // 使用现有的利润表生成服务来生成报表
        String period = request.getPeriod() != null ? request.getPeriod() : "MONTHLY_" + LocalDate.now().toString().substring(0, 7);
        
        // 调用现有的利润表生成服务
        Long profitStatementId = financialStatementService.generateProfitStatement(period, tenantId);
        
        // 创建一个对应的财务报告记录
        FinancialReport financialReport = new FinancialReport();
        financialReport.setReportNo("PL-FR-" + System.currentTimeMillis());
        financialReport.setReportType("PROFIT_STATEMENT");
        financialReport.setReportName(request.getReportName() != null ? request.getReportName() : "利润表");
        financialReport.setReportTitle(request.getReportTitle() != null ? request.getReportTitle() : "利润表");
        financialReport.setReportDate(request.getReportDate() != null ? request.getReportDate() : LocalDate.now());
        financialReport.setPeriod(period);
        financialReport.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        financialReport.setTenantId(tenantId);
        
        // 设置报表内容
        financialReport.setReportContent("Generated profit statement report for period: " + period);
        financialReport.setReportSummary("利润表汇总信息");
        financialReport.setReportAnalysis("利润表分析");
        
        financialReport.setStatus("DRAFT");
        financialReport.setVersion("1.0");
        financialReport.setCreator(getCurrentUser());
        financialReport.setDataSource("SYSTEM");
        financialReport.setGenerator("AUTO");
        
        financialReport.setCreateBy(getCurrentUser());
        financialReport.setCreateTime(LocalDateTime.now());
        financialReport.setUpdateBy(getCurrentUser());
        financialReport.setUpdateTime(LocalDateTime.now());
        
        financialReportMapper.insert(financialReport);
        return financialReport.getId();
    }

    @Override
    public Long generateCashFlowStatementReport(FinancialReportCreateRequest request, Long tenantId) {
        // 使用现有的现金流量表生成服务来生成报表
        String period = request.getPeriod() != null ? request.getPeriod() : "MONTHLY_" + LocalDate.now().toString().substring(0, 7);
        
        // 调用现有的现金流量表生成服务
        Long cashFlowStatementId = financialStatementService.generateCashFlowStatement(period, tenantId);
        
        // 创建一个对应的财务报告记录
        FinancialReport financialReport = new FinancialReport();
        financialReport.setReportNo("CF-FR-" + System.currentTimeMillis());
        financialReport.setReportType("CASH_FLOW_STATEMENT");
        financialReport.setReportName(request.getReportName() != null ? request.getReportName() : "现金流量表");
        financialReport.setReportTitle(request.getReportTitle() != null ? request.getReportTitle() : "现金流量表");
        financialReport.setReportDate(request.getReportDate() != null ? request.getReportDate() : LocalDate.now());
        financialReport.setPeriod(period);
        financialReport.setCurrency(request.getCurrency() != null ? request.getCurrency() : "CNY");
        financialReport.setTenantId(tenantId);
        
        // 设置报表内容
        financialReport.setReportContent("Generated cash flow statement report for period: " + period);
        financialReport.setReportSummary("现金流量表汇总信息");
        financialReport.setReportAnalysis("现金流量表分析");
        
        financialReport.setStatus("DRAFT");
        financialReport.setVersion("1.0");
        financialReport.setCreator(getCurrentUser());
        financialReport.setDataSource("SYSTEM");
        financialReport.setGenerator("AUTO");
        
        financialReport.setCreateBy(getCurrentUser());
        financialReport.setCreateTime(LocalDateTime.now());
        financialReport.setUpdateBy(getCurrentUser());
        financialReport.setUpdateTime(LocalDateTime.now());
        
        financialReportMapper.insert(financialReport);
        return financialReport.getId();
    }

    private FinancialReportVO convertFinancialReportToVO(FinancialReport financialReport) {
        FinancialReportVO vo = new FinancialReportVO();
        BeanUtils.copyProperties(financialReport, vo);
        return vo;
    }

    private String getCurrentUser() {
        // 获取当前用户名
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString();
        }
        return "system";
    }
}
