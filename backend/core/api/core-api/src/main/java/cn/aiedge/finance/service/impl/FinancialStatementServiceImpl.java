package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.BalanceSheet;
import cn.aiedge.finance.entity.ProfitStatement;
import cn.aiedge.finance.entity.CashFlowStatement;
import cn.aiedge.finance.mapper.BalanceSheetMapper;
import cn.aiedge.finance.mapper.ProfitStatementMapper;
import cn.aiedge.finance.mapper.CashFlowStatementMapper;
import cn.aiedge.finance.dto.*;
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
public class FinancialStatementServiceImpl implements IFinancialStatementService {

    private final BalanceSheetMapper balanceSheetMapper;
    private final ProfitStatementMapper profitStatementMapper;
    private final CashFlowStatementMapper cashFlowStatementMapper;

    public FinancialStatementServiceImpl(
            BalanceSheetMapper balanceSheetMapper,
            ProfitStatementMapper profitStatementMapper,
            CashFlowStatementMapper cashFlowStatementMapper) {
        this.balanceSheetMapper = balanceSheetMapper;
        this.profitStatementMapper = profitStatementMapper;
        this.cashFlowStatementMapper = cashFlowStatementMapper;
    }

    @Override
    public Long generateBalanceSheet(String period, Long tenantId) {
        BalanceSheet balanceSheet = new BalanceSheet();
        
        // 设置基础信息
        balanceSheet.setReportNo("BS-" + System.currentTimeMillis());
        balanceSheet.setReportDate(LocalDate.now());
        balanceSheet.setPeriod(period);
        balanceSheet.setTenantId(tenantId);
        
        // 从账务数据中汇总计算各项数值
        calculateBalanceSheetData(balanceSheet, period, tenantId);
        
        // 设置状态和审计信息
        balanceSheet.setStatus("DRAFT"); // 草稿状态
        balanceSheet.setAuditor(null);
        
        // 设置创建信息
        balanceSheet.setCreateBy(getCurrentUser());
        balanceSheet.setCreateTime(LocalDateTime.now());
        balanceSheet.setUpdateBy(getCurrentUser());
        balanceSheet.setUpdateTime(LocalDateTime.now());
        
        balanceSheetMapper.insert(balanceSheet);
        return balanceSheet.getId();
    }

    @Override
    public Long generateBalanceSheetReport(String period, Long tenantId) {
        return generateBalanceSheet(period, tenantId);
    }

    @Override
    public Page<BalanceSheetVO> pageBalanceSheets(BalanceSheetQueryRequest request) {
        LambdaQueryWrapper<BalanceSheet> wrapper = Wrappers.lambdaQuery(BalanceSheet.class)
                .eq(request.getReportDate() != null, BalanceSheet::getReportDate, request.getReportDate())
                .between(request.getStartDate() != null && request.getEndDate() != null, 
                         BalanceSheet::getReportDate, request.getStartDate(), request.getEndDate())
                .like(request.getReportNo() != null, BalanceSheet::getReportNo, request.getReportNo())
                .eq(request.getStatus() != null, BalanceSheet::getStatus, request.getStatus())
                .orderByDesc(BalanceSheet::getCreateTime);

        Page<BalanceSheet> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<BalanceSheet> resultPage = balanceSheetMapper.selectPage(page, wrapper);

        Page<BalanceSheetVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<BalanceSheetVO> voList = resultPage.getRecords().stream()
                .map(this::convertBalanceSheetToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public BalanceSheetVO getBalanceSheetById(Long id) {
        BalanceSheet balanceSheet = balanceSheetMapper.selectById(id);
        if (balanceSheet != null) {
            return convertBalanceSheetToVO(balanceSheet);
        }
        return null;
    }

    @Override
    public void deleteBalanceSheet(Long id) {
        balanceSheetMapper.deleteById(id);
    }

    @Override
    public Long generateProfitStatement(String period, Long tenantId) {
        ProfitStatement profitStatement = new ProfitStatement();
        
        // 设置基础信息
        profitStatement.setReportNo("PL-" + System.currentTimeMillis());
        profitStatement.setReportDate(LocalDate.now());
        profitStatement.setPeriod(period);
        profitStatement.setTenantId(tenantId);
        
        // 从账务数据中汇总计算各项数值
        calculateProfitStatementData(profitStatement, period, tenantId);
        
        // 设置状态和审计信息
        profitStatement.setStatus("DRAFT"); // 草稿状态
        profitStatement.setAuditor(null);
        
        // 设置创建信息
        profitStatement.setCreateBy(getCurrentUser());
        profitStatement.setCreateTime(LocalDateTime.now());
        profitStatement.setUpdateBy(getCurrentUser());
        profitStatement.setUpdateTime(LocalDateTime.now());
        
        profitStatementMapper.insert(profitStatement);
        return profitStatement.getId();
    }

    @Override
    public Page<ProfitStatementVO> pageProfitStatements(ProfitStatementQueryRequest request) {
        LambdaQueryWrapper<ProfitStatement> wrapper = Wrappers.lambdaQuery(ProfitStatement.class)
                .eq(request.getReportDate() != null, ProfitStatement::getReportDate, request.getReportDate())
                .between(request.getStartDate() != null && request.getEndDate() != null, 
                         ProfitStatement::getReportDate, request.getStartDate(), request.getEndDate())
                .like(request.getReportNo() != null, ProfitStatement::getReportNo, request.getReportNo())
                .eq(request.getStatus() != null, ProfitStatement::getStatus, request.getStatus())
                .orderByDesc(ProfitStatement::getCreateTime);

        Page<ProfitStatement> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<ProfitStatement> resultPage = profitStatementMapper.selectPage(page, wrapper);

        Page<ProfitStatementVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<ProfitStatementVO> voList = resultPage.getRecords().stream()
                .map(this::convertProfitStatementToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public ProfitStatementVO getProfitStatementById(Long id) {
        ProfitStatement profitStatement = profitStatementMapper.selectById(id);
        if (profitStatement != null) {
            return convertProfitStatementToVO(profitStatement);
        }
        return null;
    }

    @Override
    public void deleteProfitStatement(Long id) {
        profitStatementMapper.deleteById(id);
    }

    @Override
    public Long generateCashFlowStatement(String period, Long tenantId) {
        CashFlowStatement cashFlowStatement = new CashFlowStatement();
        
        // 设置基础信息
        cashFlowStatement.setReportNo("CF-" + System.currentTimeMillis());
        cashFlowStatement.setReportDate(LocalDate.now());
        cashFlowStatement.setPeriod(period);
        cashFlowStatement.setTenantId(tenantId);
        
        // 从账务数据中汇总计算各项数值
        calculateCashFlowStatementData(cashFlowStatement, period, tenantId);
        
        // 设置状态和审计信息
        cashFlowStatement.setStatus("DRAFT"); // 草稿状态
        cashFlowStatement.setAuditor(null);
        
        // 设置创建信息
        cashFlowStatement.setCreateBy(getCurrentUser());
        cashFlowStatement.setCreateTime(LocalDateTime.now());
        cashFlowStatement.setUpdateBy(getCurrentUser());
        cashFlowStatement.setUpdateTime(LocalDateTime.now());
        
        cashFlowStatementMapper.insert(cashFlowStatement);
        return cashFlowStatement.getId();
    }

    @Override
    public Page<CashFlowStatementVO> pageCashFlowStatements(CashFlowStatementQueryRequest request) {
        LambdaQueryWrapper<CashFlowStatement> wrapper = Wrappers.lambdaQuery(CashFlowStatement.class)
                .eq(request.getReportDate() != null, CashFlowStatement::getReportDate, request.getReportDate())
                .between(request.getStartDate() != null && request.getEndDate() != null, 
                         CashFlowStatement::getReportDate, request.getStartDate(), request.getEndDate())
                .like(request.getReportNo() != null, CashFlowStatement::getReportNo, request.getReportNo())
                .eq(request.getStatus() != null, CashFlowStatement::getStatus, request.getStatus())
                .orderByDesc(CashFlowStatement::getCreateTime);

        Page<CashFlowStatement> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<CashFlowStatement> resultPage = cashFlowStatementMapper.selectPage(page, wrapper);

        Page<CashFlowStatementVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<CashFlowStatementVO> voList = resultPage.getRecords().stream()
                .map(this::convertCashFlowStatementToVO)
                .collect(java.util.stream.Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public CashFlowStatementVO getCashFlowStatementById(Long id) {
        CashFlowStatement cashFlowStatement = cashFlowStatementMapper.selectById(id);
        if (cashFlowStatement != null) {
            return convertCashFlowStatementToVO(cashFlowStatement);
        }
        return null;
    }

    @Override
    public void deleteCashFlowStatement(Long id) {
        cashFlowStatementMapper.deleteById(id);
    }

    @Override
    public void auditReport(String reportType, Long id, String auditor) {
        switch (reportType.toUpperCase()) {
            case "BALANCE_SHEET":
                BalanceSheet bs = balanceSheetMapper.selectById(id);
                if (bs != null) {
                    bs.setStatus("AUDITED");
                    bs.setAuditor(auditor);
                    bs.setUpdateBy(getCurrentUser());
                    bs.setUpdateTime(LocalDateTime.now());
                    balanceSheetMapper.updateById(bs);
                }
                break;
            case "PROFIT_STATEMENT":
                ProfitStatement ps = profitStatementMapper.selectById(id);
                if (ps != null) {
                    ps.setStatus("AUDITED");
                    ps.setAuditor(auditor);
                    ps.setUpdateBy(getCurrentUser());
                    ps.setUpdateTime(LocalDateTime.now());
                    profitStatementMapper.updateById(ps);
                }
                break;
            case "CASH_FLOW_STATEMENT":
                CashFlowStatement cfs = cashFlowStatementMapper.selectById(id);
                if (cfs != null) {
                    cfs.setStatus("AUDITED");
                    cfs.setAuditor(auditor);
                    cfs.setUpdateBy(getCurrentUser());
                    cfs.setUpdateTime(LocalDateTime.now());
                    cashFlowStatementMapper.updateById(cfs);
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的报表类型: " + reportType);
        }
    }

    @Override
    public void batchGenerateReports(String period, Long tenantId) {
        // 批量生成三大报表
        generateBalanceSheet(period, tenantId);
        generateProfitStatement(period, tenantId);
        generateCashFlowStatement(period, tenantId);
    }

    // 资产负债表数据计算方法
    private void calculateBalanceSheetData(BalanceSheet balanceSheet, String period, Long tenantId) {
        // 这里应该是从会计科目、总账、明细账等数据中汇总计算
        // 为了示例，我们设置一些默认值，实际应用中应从数据库汇总
        balanceSheet.setCashAndCashEquivalents(BigDecimal.ZERO);
        balanceSheet.setAccountsReceivable(BigDecimal.ZERO);
        balanceSheet.setInventory(BigDecimal.ZERO);
        balanceSheet.setPrepaidExpenses(BigDecimal.ZERO);
        balanceSheet.setOtherCurrentAssets(BigDecimal.ZERO);
        
        // 计算流动资产合计
        BigDecimal totalCurrentAssets = balanceSheet.getCashAndCashEquivalents()
                .add(balanceSheet.getAccountsReceivable())
                .add(balanceSheet.getInventory())
                .add(balanceSheet.getPrepaidExpenses())
                .add(balanceSheet.getOtherCurrentAssets());
        balanceSheet.setTotalCurrentAssets(totalCurrentAssets);
        
        balanceSheet.setFixedAssets(BigDecimal.ZERO);
        balanceSheet.setConstructionInProgress(BigDecimal.ZERO);
        balanceSheet.setIntangibleAssets(BigDecimal.ZERO);
        balanceSheet.setLongTermInvestments(BigDecimal.ZERO);
        balanceSheet.setOtherNonCurrentAssets(BigDecimal.ZERO);
        
        // 计算非流动资产合计
        BigDecimal totalNonCurrentAssets = balanceSheet.getFixedAssets()
                .add(balanceSheet.getConstructionInProgress())
                .add(balanceSheet.getIntangibleAssets())
                .add(balanceSheet.getLongTermInvestments())
                .add(balanceSheet.getOtherNonCurrentAssets());
        balanceSheet.setTotalNonCurrentAssets(totalNonCurrentAssets);
        
        // 计算资产总计
        BigDecimal totalAssets = totalCurrentAssets.add(totalNonCurrentAssets);
        balanceSheet.setTotalAssets(totalAssets);
        
        // 负债部分
        balanceSheet.setShortTermLoans(BigDecimal.ZERO);
        balanceSheet.setAccountsPayable(BigDecimal.ZERO);
        balanceSheet.setAdvancesFromCustomers(BigDecimal.ZERO);
        balanceSheet.setSalariesPayable(BigDecimal.ZERO);
        balanceSheet.setTaxesPayable(BigDecimal.ZERO);
        balanceSheet.setOtherCurrentLiabilities(BigDecimal.ZERO);
        
        // 计算流动负债合计
        BigDecimal totalCurrentLiabilities = balanceSheet.getShortTermLoans()
                .add(balanceSheet.getAccountsPayable())
                .add(balanceSheet.getAdvancesFromCustomers())
                .add(balanceSheet.getSalariesPayable())
                .add(balanceSheet.getTaxesPayable())
                .add(balanceSheet.getOtherCurrentLiabilities());
        balanceSheet.setTotalCurrentLiabilities(totalCurrentLiabilities);
        
        balanceSheet.setLongTermLoans(BigDecimal.ZERO);
        balanceSheet.setBondsPayable(BigDecimal.ZERO);
        balanceSheet.setOtherNonCurrentLiabilities(BigDecimal.ZERO);
        
        // 计算非流动负债合计
        BigDecimal totalNonCurrentLiabilities = balanceSheet.getLongTermLoans()
                .add(balanceSheet.getBondsPayable())
                .add(balanceSheet.getOtherNonCurrentLiabilities());
        balanceSheet.setTotalNonCurrentLiabilities(totalNonCurrentLiabilities);
        
        // 计算负债合计
        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalNonCurrentLiabilities);
        balanceSheet.setTotalLiabilities(totalLiabilities);
        
        // 所有者权益部分
        balanceSheet.setPaidInCapital(BigDecimal.ZERO);
        balanceSheet.setCapitalReserve(BigDecimal.ZERO);
        balanceSheet.setSurplusReserve(BigDecimal.ZERO);
        balanceSheet.setUndistributedProfits(BigDecimal.ZERO);
        
        // 计算所有者权益合计
        BigDecimal totalOwnersEquity = balanceSheet.getPaidInCapital()
                .add(balanceSheet.getCapitalReserve())
                .add(balanceSheet.getSurplusReserve())
                .add(balanceSheet.getUndistributedProfits());
        balanceSheet.setTotalOwnersEquity(totalOwnersEquity);
        
        // 计算负债和所有者权益总计
        BigDecimal totalLiabilitiesAndOwnersEquity = totalLiabilities.add(totalOwnersEquity);
        balanceSheet.setTotalLiabilitiesAndOwnersEquity(totalLiabilitiesAndOwnersEquity);
    }

    // 利润表数据计算方法
    private void calculateProfitStatementData(ProfitStatement profitStatement, String period, Long tenantId) {
        // 这里应该是从会计科目、总账、明细账等数据中汇总计算
        // 为了示例，我们设置一些默认值，实际应用中应从数据库汇总
        profitStatement.setOperatingRevenue(BigDecimal.ZERO);
        profitStatement.setOtherIncome(BigDecimal.ZERO);
        
        // 计算收入合计
        BigDecimal totalIncome = profitStatement.getOperatingRevenue()
                .add(profitStatement.getOtherIncome());
        profitStatement.setTotalIncome(totalIncome);
        
        // 成本费用部分
        profitStatement.setOperatingCosts(BigDecimal.ZERO);
        profitStatement.setTaxesAndSurcharges(BigDecimal.ZERO);
        profitStatement.setSalesExpenses(BigDecimal.ZERO);
        profitStatement.setAdministrativeExpenses(BigDecimal.ZERO);
        profitStatement.setFinancialExpenses(BigDecimal.ZERO);
        profitStatement.setAssetImpairmentLosses(BigDecimal.ZERO);
        profitStatement.setCreditImpairmentLosses(BigDecimal.ZERO);
        profitStatement.setOtherExpenses(BigDecimal.ZERO);
        
        // 计算费用合计
        BigDecimal totalExpenses = profitStatement.getOperatingCosts()
                .add(profitStatement.getTaxesAndSurcharges())
                .add(profitStatement.getSalesExpenses())
                .add(profitStatement.getAdministrativeExpenses())
                .add(profitStatement.getFinancialExpenses())
                .add(profitStatement.getAssetImpairmentLosses())
                .add(profitStatement.getCreditImpairmentLosses())
                .add(profitStatement.getOtherExpenses());
        profitStatement.setTotalExpenses(totalExpenses);
        
        // 计算营业利润
        BigDecimal grossProfit = totalIncome.subtract(totalExpenses);
        profitStatement.setGrossProfit(grossProfit);
        
        // 其他收支项目
        profitStatement.setInvestmentIncome(BigDecimal.ZERO);
        profitStatement.setNonOperatingIncome(BigDecimal.ZERO);
        profitStatement.setNonOperatingExpenses(BigDecimal.ZERO);
        
        // 计算利润总额
        BigDecimal totalProfit = grossProfit
                .add(profitStatement.getInvestmentIncome())
                .add(profitStatement.getNonOperatingIncome())
                .subtract(profitStatement.getNonOperatingExpenses());
        profitStatement.setTotalProfit(totalProfit);
        
        // 所得税和净利润
        profitStatement.setIncomeTaxExpense(BigDecimal.ZERO);
        
        // 计算净利润
        BigDecimal netProfit = totalProfit.subtract(profitStatement.getIncomeTaxExpense());
        profitStatement.setNetProfit(netProfit);
        
        // 每股收益（示例值）
        profitStatement.setBasicEarningsPerShare(BigDecimal.ZERO);
        profitStatement.setDilutedEarningsPerShare(BigDecimal.ZERO);
    }

    // 现金流量表数据计算方法
    private void calculateCashFlowStatementData(CashFlowStatement cashFlowStatement, String period, Long tenantId) {
        // 这里应该是从会计科目、总账、明细账等数据中汇总计算
        // 为了示例，我们设置一些默认值，实际应用中应从数据库汇总
        cashFlowStatement.setCashReceivedFromSales(BigDecimal.ZERO);
        cashFlowStatement.setTaxRefundsReceived(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashReceivedFromOperating(BigDecimal.ZERO);
        cashFlowStatement.setCashPaidForGoods(BigDecimal.ZERO);
        cashFlowStatement.setCashPaidToEmployees(BigDecimal.ZERO);
        cashFlowStatement.setTaxPayments(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashPaidForOperating(BigDecimal.ZERO);
        
        // 计算经营活动现金流量净额
        BigDecimal netCashFromOperatingActivities = cashFlowStatement.getCashReceivedFromSales()
                .add(cashFlowStatement.getTaxRefundsReceived())
                .add(cashFlowStatement.getOtherCashReceivedFromOperating())
                .subtract(cashFlowStatement.getCashPaidForGoods())
                .subtract(cashFlowStatement.getCashPaidToEmployees())
                .subtract(cashFlowStatement.getTaxPayments())
                .subtract(cashFlowStatement.getOtherCashPaidForOperating());
        cashFlowStatement.setNetCashFromOperatingActivities(netCashFromOperatingActivities);
        
        // 投资活动现金流
        cashFlowStatement.setProceedsFromDisposalOfInvestments(BigDecimal.ZERO);
        cashFlowStatement.setInvestmentIncomeReceived(BigDecimal.ZERO);
        cashFlowStatement.setProceedsFromDisposalOfFixedAssets(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashReceivedFromInvesting(BigDecimal.ZERO);
        cashFlowStatement.setCashPaidForInvestments(BigDecimal.ZERO);
        cashFlowStatement.setCashPaidForAcquisitionOfFixedAssets(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashPaidForInvesting(BigDecimal.ZERO);
        
        // 计算投资活动现金流量净额
        BigDecimal netCashFromInvestingActivities = cashFlowStatement.getProceedsFromDisposalOfInvestments()
                .add(cashFlowStatement.getInvestmentIncomeReceived())
                .add(cashFlowStatement.getProceedsFromDisposalOfFixedAssets())
                .add(cashFlowStatement.getOtherCashReceivedFromInvesting())
                .subtract(cashFlowStatement.getCashPaidForInvestments())
                .subtract(cashFlowStatement.getCashPaidForAcquisitionOfFixedAssets())
                .subtract(cashFlowStatement.getOtherCashPaidForInvesting());
        cashFlowStatement.setNetCashFromInvestingActivities(netCashFromInvestingActivities);
        
        // 筹资活动现金流
        cashFlowStatement.setCashReceivedFromInvestors(BigDecimal.ZERO);
        cashFlowStatement.setBorrowingsReceived(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashReceivedFromFinancing(BigDecimal.ZERO);
        cashFlowStatement.setRepaymentsOfPrincipal(BigDecimal.ZERO);
        cashFlowStatement.setDividendInterestPayments(BigDecimal.ZERO);
        cashFlowStatement.setOtherCashPaidForFinancing(BigDecimal.ZERO);
        
        // 计算筹资活动现金流量净额
        BigDecimal netCashFromFinancingActivities = cashFlowStatement.getCashReceivedFromInvestors()
                .add(cashFlowStatement.getBorrowingsReceived())
                .add(cashFlowStatement.getOtherCashReceivedFromFinancing())
                .subtract(cashFlowStatement.getRepaymentsOfPrincipal())
                .subtract(cashFlowStatement.getDividendInterestPayments())
                .subtract(cashFlowStatement.getOtherCashPaidForFinancing());
        cashFlowStatement.setNetCashFromFinancingActivities(netCashFromFinancingActivities);
        
        // 其他项目
        cashFlowStatement.setExchangeRateEffect(BigDecimal.ZERO);
        
        // 计算现金及现金等价物净增加额
        BigDecimal netIncreaseInCash = netCashFromOperatingActivities
                .add(netCashFromInvestingActivities)
                .add(netCashFromFinancingActivities)
                .add(cashFlowStatement.getExchangeRateEffect());
        cashFlowStatement.setNetIncreaseInCash(netIncreaseInCash);
        
        cashFlowStatement.setBeginningCashBalance(BigDecimal.ZERO);
        
        // 计算期末现金余额
        BigDecimal endingCashBalance = cashFlowStatement.getBeginningCashBalance()
                .add(cashFlowStatement.getNetIncreaseInCash());
        cashFlowStatement.setEndingCashBalance(endingCashBalance);
    }

    private BalanceSheetVO convertBalanceSheetToVO(BalanceSheet balanceSheet) {
        BalanceSheetVO vo = new BalanceSheetVO();
        BeanUtils.copyProperties(balanceSheet, vo);
        return vo;
    }

    private ProfitStatementVO convertProfitStatementToVO(ProfitStatement profitStatement) {
        ProfitStatementVO vo = new ProfitStatementVO();
        BeanUtils.copyProperties(profitStatement, vo);
        return vo;
    }

    private CashFlowStatementVO convertCashFlowStatementToVO(CashFlowStatement cashFlowStatement) {
        CashFlowStatementVO vo = new CashFlowStatementVO();
        BeanUtils.copyProperties(cashFlowStatement, vo);
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
