package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.LedgerEntryMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import cn.aiedge.erp.finance.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务报表Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialReportServiceImpl implements FinancialReportService {

    private final LedgerEntryMapper ledgerEntryMapper;
    private final AccountSubjectMapper accountSubjectMapper;

    @Override
    public List<TrialBalanceDTO> generateTrialBalance(Integer fiscalYear, Integer fiscalPeriod) {
        List<LedgerEntry> entries = ledgerEntryMapper.findByFiscalYear(fiscalYear).stream()
                .filter(e -> e.getFiscalPeriod() != null && e.getFiscalPeriod().equals(fiscalPeriod))
                .collect(Collectors.toList());

        if (entries.isEmpty()) {
            log.warn("试算平衡表无数据: year={}, period={}", fiscalYear, fiscalPeriod);
            return new ArrayList<>();
        }

        List<TrialBalanceDTO> result = entries.stream()
                .map(this::toTrialBalanceDTO)
                .sorted(Comparator.comparing(
                        dto -> dto.getSubjectCode() != null ? dto.getSubjectCode() : ""))
                .collect(Collectors.toList());

        log.info("生成试算平衡表: year={}, period={}, items={}", fiscalYear, fiscalPeriod, result.size());
        return result;
    }

    @Override
    public List<BalanceSheetDTO> generateBalanceSheet(Integer fiscalYear, Integer fiscalPeriod) {
        List<LedgerEntry> periodEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear).stream()
                .filter(e -> e.getFiscalPeriod() != null && e.getFiscalPeriod().equals(fiscalPeriod))
                .collect(Collectors.toList());

        List<AccountSubject> allSubjects = accountSubjectMapper.selectList(null);
        List<BalanceSheetDTO> result = new ArrayList<>();

        BigDecimal totalAssetsEnd = BigDecimal.ZERO;
        BigDecimal totalLiabilitiesEnd = BigDecimal.ZERO;
        BigDecimal totalEquityEnd = BigDecimal.ZERO;
        BigDecimal totalAssetsBegin = BigDecimal.ZERO;
        BigDecimal totalLiabilitiesBegin = BigDecimal.ZERO;
        BigDecimal totalEquityBegin = BigDecimal.ZERO;

        // 资产类 (1xxx)
        List<AccountSubject> assetSubjects = filterSubjectsByType(allSubjects, 1);
        for (AccountSubject subject : assetSubjects) {
            LedgerEntry entry = findEntry(periodEntries, subject.getId());
            if (entry == null) continue;
            BigDecimal endBalance = getSubjectBalance(entry, 1);
            BigDecimal beginBalance = (entry.getOpeningDebit() != null ? entry.getOpeningDebit() : BigDecimal.ZERO)
                    .subtract(entry.getOpeningCredit() != null ? entry.getOpeningCredit() : BigDecimal.ZERO);
            if (beginBalance.compareTo(BigDecimal.ZERO) < 0) beginBalance = BigDecimal.ZERO;

            if (endBalance.compareTo(BigDecimal.ZERO) != 0 || beginBalance.compareTo(BigDecimal.ZERO) != 0) {
                result.add(new BalanceSheetDTO(subject.getSubjectCode(), subject.getSubjectName(),
                        null, subject.getLevel(), endBalance, beginBalance, "asset"));
                totalAssetsEnd = totalAssetsEnd.add(endBalance);
                totalAssetsBegin = totalAssetsBegin.add(beginBalance);
            }
        }

        // 负债类 (2xxx)
        List<AccountSubject> liabilitySubjects = filterSubjectsByType(allSubjects, 2);
        for (AccountSubject subject : liabilitySubjects) {
            LedgerEntry entry = findEntry(periodEntries, subject.getId());
            if (entry == null) continue;
            BigDecimal endBalance = getSubjectBalance(entry, 2);
            BigDecimal beginBalance = (entry.getOpeningCredit() != null ? entry.getOpeningCredit() : BigDecimal.ZERO)
                    .subtract(entry.getOpeningDebit() != null ? entry.getOpeningDebit() : BigDecimal.ZERO);
            if (beginBalance.compareTo(BigDecimal.ZERO) < 0) beginBalance = BigDecimal.ZERO;

            if (endBalance.compareTo(BigDecimal.ZERO) != 0 || beginBalance.compareTo(BigDecimal.ZERO) != 0) {
                result.add(new BalanceSheetDTO(subject.getSubjectCode(), subject.getSubjectName(),
                        null, subject.getLevel(), endBalance, beginBalance, "liability"));
                totalLiabilitiesEnd = totalLiabilitiesEnd.add(endBalance);
                totalLiabilitiesBegin = totalLiabilitiesBegin.add(beginBalance);
            }
        }

        // 权益类 (4xxx)
        List<AccountSubject> equitySubjects = filterSubjectsByType(allSubjects, 3);
        for (AccountSubject subject : equitySubjects) {
            LedgerEntry entry = findEntry(periodEntries, subject.getId());
            if (entry == null) continue;
            BigDecimal endBalance = getSubjectBalance(entry, 3);
            BigDecimal beginBalance = (entry.getOpeningCredit() != null ? entry.getOpeningCredit() : BigDecimal.ZERO)
                    .subtract(entry.getOpeningDebit() != null ? entry.getOpeningDebit() : BigDecimal.ZERO);
            if (beginBalance.compareTo(BigDecimal.ZERO) < 0) beginBalance = BigDecimal.ZERO;

            if (endBalance.compareTo(BigDecimal.ZERO) != 0 || beginBalance.compareTo(BigDecimal.ZERO) != 0) {
                result.add(new BalanceSheetDTO(subject.getSubjectCode(), subject.getSubjectName(),
                        null, subject.getLevel(), endBalance, beginBalance, "equity"));
                totalEquityEnd = totalEquityEnd.add(endBalance);
                totalEquityBegin = totalEquityBegin.add(beginBalance);
            }
        }

        // 添加合计行
        result.add(new BalanceSheetDTO("TOTAL_ASSETS", "资产合计", null, 0,
                totalAssetsEnd, totalAssetsBegin, "asset"));
        result.add(new BalanceSheetDTO("TOTAL_LIABILITIES", "负债合计", null, 0,
                totalLiabilitiesEnd, totalLiabilitiesBegin, "liability"));
        result.add(new BalanceSheetDTO("TOTAL_EQUITY", "所有者权益合计", null, 0,
                totalEquityEnd, totalEquityBegin, "equity"));
        result.add(new BalanceSheetDTO("TOTAL_LIABILITIES_EQUITY", "负债和所有者权益总计", null, 0,
                totalLiabilitiesEnd.add(totalEquityEnd), totalLiabilitiesBegin.add(totalEquityBegin), "liability_equity"));

        log.info("生成资产负债表: year={}, period={}, items={}", fiscalYear, fiscalPeriod, result.size());
        return result;
    }

    @Override
    public List<IncomeStatementDTO> generateIncomeStatement(Integer fiscalYear, Integer fiscalPeriod,
                                                             Integer startMonth, Integer endMonth) {
        List<LedgerEntry> yearEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear);
        List<AccountSubject> allSubjects = accountSubjectMapper.selectList(null);
        List<IncomeStatementDTO> result = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        // 收入类科目 (6xxx 收入类)
        String[] revenueCodes = {"6001", "6051", "6111", "6301"};
        for (String code : revenueCodes) {
            IncomeStatementDTO item = buildIncomeItem(yearEntries, allSubjects, code,
                    startMonth, endMonth, "revenue");
            if (item != null) {
                result.add(item);
                totalRevenue = totalRevenue.add(item.getCurrentAmount() != null ? item.getCurrentAmount() : BigDecimal.ZERO);
            }
        }

        // 成本类科目 (6401, 6402, 6403)
        String[] costCodes = {"6401", "6402", "6403"};
        for (String code : costCodes) {
            IncomeStatementDTO item = buildIncomeItem(yearEntries, allSubjects, code,
                    startMonth, endMonth, "cost");
            if (item != null) {
                result.add(item);
                totalCost = totalCost.add(item.getCurrentAmount() != null ? item.getCurrentAmount() : BigDecimal.ZERO);
            }
        }

        // 费用类科目 (6601, 6602, 6603, 6701, 6711)
        String[] expenseCodes = {"6601", "6602", "6603", "6701", "6711"};
        for (String code : expenseCodes) {
            IncomeStatementDTO item = buildIncomeItem(yearEntries, allSubjects, code,
                    startMonth, endMonth, "expense");
            if (item != null) {
                result.add(item);
                totalExpense = totalExpense.add(item.getCurrentAmount() != null ? item.getCurrentAmount() : BigDecimal.ZERO);
            }
        }

        // 合计行
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal netProfit = grossProfit.subtract(totalExpense).subtract(totalTax);

        result.add(new IncomeStatementDTO("TOTAL_REVENUE", "营业收入合计", null, 0,
                totalRevenue, totalRevenue));
        result.add(new IncomeStatementDTO("TOTAL_COST", "营业成本合计", null, 0,
                totalCost, totalCost));
        result.add(new IncomeStatementDTO("GROSS_PROFIT", "营业毛利", null, 0,
                grossProfit, grossProfit));
        result.add(new IncomeStatementDTO("TOTAL_EXPENSE", "期间费用合计", null, 0,
                totalExpense, totalExpense));
        result.add(new IncomeStatementDTO("NET_PROFIT", "净利润", null, 0,
                netProfit, netProfit));

        log.info("生成利润表: year={}, period={}, startMonth={}, endMonth={}",
                fiscalYear, fiscalPeriod, startMonth, endMonth);
        return result;
    }

    @Override
    public Map<String, Object> getDashboardKPIs() {
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();

        List<LedgerEntry> currentEntries = ledgerEntryMapper.findByFiscalYear(currentYear).stream()
                .filter(e -> e.getFiscalPeriod().intValue() == currentMonth)
                .collect(Collectors.toList());
        List<LedgerEntry> yearEntries = ledgerEntryMapper.findByFiscalYear(currentYear);
        List<AccountSubject> allSubjects = accountSubjectMapper.selectList(null);

        BigDecimal totalAssets = currentEntries.stream()
                .filter(e -> isSubjectType(allSubjects, e.getSubjectId(), 1))
                .map(e -> e.getClosingDebit() != null ? e.getClosingDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = currentEntries.stream()
                .filter(e -> isSubjectType(allSubjects, e.getSubjectId(), 2))
                .map(e -> e.getClosingCredit() != null ? e.getClosingCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = yearEntries.stream()
                .filter(e -> isSubjectCodeMatch(allSubjects, e.getSubjectId(), "6001", "6051", "6111", "6301"))
                .map(e -> e.getPeriodCredit() != null ? e.getPeriodCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = yearEntries.stream()
                .filter(e -> isSubjectCodeMatch(allSubjects, e.getSubjectId(), "6601", "6602", "6603", "6701", "6711"))
                .map(e -> e.getPeriodDebit() != null ? e.getPeriodDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = yearEntries.stream()
                .filter(e -> isSubjectCodeMatch(allSubjects, e.getSubjectId(), "6401", "6402", "6403"))
                .map(e -> e.getPeriodDebit() != null ? e.getPeriodDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profit = totalRevenue.subtract(totalCost).subtract(totalExpense);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalAssets", totalAssets);
        kpis.put("totalLiabilities", totalLiabilities);
        kpis.put("totalRevenue", totalRevenue);
        kpis.put("totalExpense", totalExpense);
        kpis.put("totalCost", totalCost);
        kpis.put("netProfit", profit);
        kpis.put("fiscalYear", currentYear);
        kpis.put("fiscalPeriod", currentMonth);

        return kpis;
    }

    // ======== 辅助方法 ========

    private List<AccountSubject> filterSubjectsByType(List<AccountSubject> subjects, int subjectType) {
        return subjects.stream()
                .filter(s -> s.getSubjectType().intValue() == subjectType && s.getDeletedFlag() == 0)
                .sorted(Comparator.comparing(AccountSubject::getSubjectCode))
                .collect(Collectors.toList());
    }

    private LedgerEntry findEntry(List<LedgerEntry> entries, Long subjectId) {
        return entries.stream()
                .filter(e -> e.getSubjectId().equals(subjectId))
                .findFirst().orElse(null);
    }

    private BigDecimal getSubjectBalance(LedgerEntry entry, int subjectType) {
        if (entry == null) return BigDecimal.ZERO;
        if (subjectType == 1) {
            return entry.getClosingDebit() != null ? entry.getClosingDebit() : BigDecimal.ZERO;
        } else {
            return entry.getClosingCredit() != null ? entry.getClosingCredit() : BigDecimal.ZERO;
        }
    }

    private IncomeStatementDTO buildIncomeItem(List<LedgerEntry> yearEntries, List<AccountSubject> subjects,
                                                String subjectCode, Integer startMonth, Integer endMonth,
                                                String category) {
        AccountSubject subject = subjects.stream()
                .filter(s -> subjectCode.equals(s.getSubjectCode()) && s.getDeletedFlag() == 0)
                .findFirst().orElse(null);
        if (subject == null) return null;

        BigDecimal currentAmount = yearEntries.stream()
                .filter(e -> e.getSubjectId().equals(subject.getId())
                        && e.getFiscalPeriod() >= startMonth
                        && e.getFiscalPeriod() <= endMonth)
                .map(e -> {
                    if ("revenue".equals(category)) {
                        return e.getPeriodCredit() != null ? e.getPeriodCredit() : BigDecimal.ZERO;
                    } else {
                        return e.getPeriodDebit() != null ? e.getPeriodDebit() : BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cumulativeAmount = yearEntries.stream()
                .filter(e -> e.getSubjectId().equals(subject.getId())
                        && e.getFiscalPeriod() <= endMonth)
                .map(e -> {
                    if ("revenue".equals(category)) {
                        return e.getPeriodCredit() != null ? e.getPeriodCredit() : BigDecimal.ZERO;
                    } else {
                        return e.getPeriodDebit() != null ? e.getPeriodDebit() : BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new IncomeStatementDTO(subjectCode, subject.getSubjectName(),
                null, subject.getLevel(), currentAmount, cumulativeAmount);
    }

    private boolean isSubjectType(List<AccountSubject> subjects, Long subjectId, int type) {
        return subjects.stream()
                .anyMatch(s -> s.getId().equals(subjectId) && s.getSubjectType().intValue() == type);
    }

    private boolean isSubjectCodeMatch(List<AccountSubject> subjects, Long subjectId, String... codes) {
        return subjects.stream()
                .anyMatch(s -> s.getId().equals(subjectId) && List.of(codes).contains(s.getSubjectCode()));
    }

    private TrialBalanceDTO toTrialBalanceDTO(LedgerEntry entry) {
        TrialBalanceDTO dto = new TrialBalanceDTO();
        dto.setSubjectCode(entry.getSubjectCode());
        dto.setSubjectName(entry.getSubjectName());
        dto.setOpeningDebit(entry.getOpeningDebit());
        dto.setOpeningCredit(entry.getOpeningCredit());
        dto.setPeriodDebit(entry.getPeriodDebit());
        dto.setPeriodCredit(entry.getPeriodCredit());
        dto.setClosingDebit(entry.getClosingDebit());
        dto.setClosingCredit(entry.getClosingCredit());
        return dto;
    }
}
