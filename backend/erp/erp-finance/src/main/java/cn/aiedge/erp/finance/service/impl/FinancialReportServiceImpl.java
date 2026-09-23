package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.BalanceSheetQuery;
import cn.aiedge.erp.finance.dto.BalanceSheetReportDTO;
import cn.aiedge.erp.finance.dto.BalanceSheetRowDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementQuery;
import cn.aiedge.erp.finance.dto.IncomeStatementReportDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementRowDTO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // ======== 资产负债表（左右对照 · 金标准） ========

    /** 资产侧固定项目行：{科目编码前缀, 项目名称}，按顺序互斥匹配 */
    private static final String[][] ASSET_LINE_DEFS = {
            {"1001", "现金库存"},
            {"1002", "银行存款"},
            {"1122", "应收账款"},
            {"1123", "预付账款"},
            {"140", "库存商品"},
            {"160", "固定资产"},
    };

    /** 负债侧固定项目行：末行前缀为空 = 其余负债科目（预订货款） */
    private static final String[][] LIABILITY_LINE_DEFS = {
            {"2202", "应付账款"},
            {"2203", "预收账款"},
            {"", "预订货款"},
    };

    private static final String ASSET_TOTAL_NAME = "资产合计";
    private static final String LIABILITY_TOTAL_NAME = "负债合计";
    private static final String EQUITY_TOTAL_NAME = "所有者权益合计";
    private static final String LIABILITY_EQUITY_TOTAL_NAME = "负债和所有者权益总计";
    private static final String UNDISTRIBUTED_PROFIT_NAME = "未分配利润";
    private static final String CAPITAL_NAME = "期初资本";
    /** 利润分配类科目前缀：4103 本年利润 / 4104 利润分配 */
    private static final String RETAINED_EARNINGS_PREFIX = "41";
    /** 平衡判定容差 */
    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.01");

    @Override
    public BalanceSheetReportDTO generateBalanceSheetReport(BalanceSheetQuery query) {
        int fiscalYear = query.getFiscalYear() != null
                ? query.getFiscalYear() : java.time.LocalDate.now().getYear();
        int endPeriod = query.resolveEndPeriod();
        int subjectLevel = query.getSubjectLevel() != null ? query.getSubjectLevel() : 1;
        boolean showZero = !Boolean.FALSE.equals(query.getShowZero());

        List<LedgerEntry> yearEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear);
        // 只读取科目余额（finance_ledger），不绕过凭证直改
        Map<Long, SubjectBalance> ownBalances = aggregateSubjectBalances(yearEntries, endPeriod);

        // 层级内科目：父科目余额 = 自有余额 + 全部子科目余额，避免父子重复或漏计
        List<AccountSubject> scopedSubjects = loadReportSubjects(subjectLevel);
        Map<Long, SubjectBalance> balances = rollupSubjectBalances(scopedSubjects, ownBalances);
        List<AccountSubject> subjects = topLevelSubjects(scopedSubjects);

        List<AccountSubject> assets = filterByType(subjects, 1);
        List<AccountSubject> liabilities = filterByType(subjects, 2);
        List<AccountSubject> equities = filterByType(subjects, 3);
        List<AccountSubject> profits = subjects.stream()
                .filter(s -> s.getSubjectType() == 4 || s.getSubjectType() == 5)
                .collect(Collectors.toList());

        // ── 左：资产侧 ──
        List<ReportItem> assetItems = buildSideItems(ASSET_LINE_DEFS, assets, balances, true);
        BigDecimal assetBeginTotal = sumItems(assetItems, true);
        BigDecimal assetEndTotal = sumItems(assetItems, false);
        assetItems.add(new ReportItem(ASSET_TOTAL_NAME, assetBeginTotal, assetEndTotal, true, null, null));

        // ── 右：负债 + 所有者权益侧 ──
        List<ReportItem> liabilityItems = buildSideItems(LIABILITY_LINE_DEFS, liabilities, balances, false);
        BigDecimal liabilityBeginTotal = sumItems(liabilityItems, true);
        BigDecimal liabilityEndTotal = sumItems(liabilityItems, false);
        liabilityItems.add(new ReportItem(LIABILITY_TOTAL_NAME, liabilityBeginTotal, liabilityEndTotal, true, null, null));

        List<ReportItem> equityItems = buildEquityItems(equities, profits, balances);
        BigDecimal equityBeginTotal = sumItems(equityItems, true);
        BigDecimal equityEndTotal = sumItems(equityItems, false);
        equityItems.add(new ReportItem(EQUITY_TOTAL_NAME, equityBeginTotal, equityEndTotal, true, null, null));
        equityItems.add(new ReportItem(LIABILITY_EQUITY_TOTAL_NAME,
                liabilityBeginTotal.add(equityBeginTotal), liabilityEndTotal.add(equityEndTotal), true, null, null));

        List<ReportItem> rightItems = new ArrayList<>(liabilityItems);
        rightItems.addAll(equityItems);

        // ── 组装左右对照行（按行号对齐） ──
        List<ReportItem> left = filterZeroItems(assetItems, showZero);
        List<ReportItem> right = filterZeroItems(rightItems, showZero);

        BalanceSheetReportDTO report = new BalanceSheetReportDTO();
        report.setFiscalYear(fiscalYear);
        report.setFiscalPeriod(endPeriod);
        report.setPeriodMode(query.isMultiPeriod() ? "multi" : "single");
        report.setStartPeriod(query.resolveStartPeriod());
        report.setEndPeriod(endPeriod);
        report.setSubjectLevel(subjectLevel);
        report.setShowZero(showZero);
        report.setRows(mergeRows(left, right));

        report.setAssetBeginTotal(assetBeginTotal);
        report.setAssetEndTotal(assetEndTotal);
        report.setLiabilityBeginTotal(liabilityBeginTotal);
        report.setLiabilityEndTotal(liabilityEndTotal);
        report.setEquityBeginTotal(equityBeginTotal);
        report.setEquityEndTotal(equityEndTotal);
        report.setLiabilityEquityBeginTotal(liabilityBeginTotal.add(equityBeginTotal));
        report.setLiabilityEquityEndTotal(liabilityEndTotal.add(equityEndTotal));

        BigDecimal difference = assetEndTotal.subtract(liabilityEndTotal).subtract(equityEndTotal);
        report.setDifference(difference);
        report.setBalanced(difference.abs().compareTo(BALANCE_TOLERANCE) < 0);
        report.setHasData(!yearEntries.isEmpty());

        log.info("生成资产负债表: year={}, period={}, mode={}, rows={}, 资产={}, 负债={}, 权益={}, 平衡={}",
                fiscalYear, endPeriod, report.getPeriodMode(), report.getRows().size(),
                assetEndTotal, liabilityEndTotal, equityEndTotal, report.getBalanced());
        return report;
    }

    /**
     * 汇总每个科目的期末与年初余额，统一为「借方 - 贷方」口径
     *
     * 期末余额 = 年初余额 + 各期发生额累计：不依赖跨期结转链，避免凭证补录到历史期间后，
     * 后续期间未重算导致期末余额漏计、报表失衡（只要凭证借贷平衡，报表即平衡）。
     * 年初余额 = 本年度第 1 期期初余额。
     */
    private Map<Long, SubjectBalance> aggregateSubjectBalances(List<LedgerEntry> entries, int endPeriod) {
        Map<Long, SubjectBalance> map = new HashMap<>();
        for (LedgerEntry e : entries) {
            if (e.getSubjectId() == null || e.getFiscalPeriod() == null) continue;
            int period = e.getFiscalPeriod();
            if (period > endPeriod) continue;
            SubjectBalance sb = map.computeIfAbsent(e.getSubjectId(), k -> new SubjectBalance());
            if (period == 1 && sb.beginPeriod == null) {
                sb.beginPeriod = 1;
                sb.beginDebitMinusCredit = nz(e.getOpeningDebit()).subtract(nz(e.getOpeningCredit()));
            }
            sb.endDebitMinusCredit = sb.endDebitMinusCredit
                    .add(nz(e.getPeriodDebit()).subtract(nz(e.getPeriodCredit())));
        }
        for (SubjectBalance sb : map.values()) {
            sb.endDebitMinusCredit = sb.endDebitMinusCredit.add(sb.beginDebitMinusCredit);
        }
        return map;
    }

    /** 报表参与科目：启用的、层级范围内的科目（父子关系由 rollupSubjectBalances 处理） */
    private List<AccountSubject> loadReportSubjects(int subjectLevel) {
        return accountSubjectMapper.selectList(null).stream()
                .filter(s -> s.getSubjectType() != null)
                .filter(s -> s.getIsEnabled() == null || Boolean.TRUE.equals(s.getIsEnabled()))
                .filter(s -> s.getLevel() == null || s.getLevel() <= subjectLevel)
                .sorted(Comparator.comparing(AccountSubject::getSubjectCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** 顶级科目：父科目不在层级范围内的科目，其余额已汇总全部下级 */
    private List<AccountSubject> topLevelSubjects(List<AccountSubject> scoped) {
        Set<Long> ids = scoped.stream().map(AccountSubject::getId).collect(Collectors.toSet());
        return scoped.stream()
                .filter(s -> s.getParentId() == null || !ids.contains(s.getParentId()))
                .collect(Collectors.toList());
    }

    /** 父科目余额 = 自有余额 + 全部子科目余额（子科目同样递归汇总） */
    private Map<Long, SubjectBalance> rollupSubjectBalances(List<AccountSubject> scoped,
                                                            Map<Long, SubjectBalance> own) {
        Map<Long, AccountSubject> byId = scoped.stream()
                .collect(Collectors.toMap(AccountSubject::getId, s -> s, (a, b) -> a));
        Map<Long, List<AccountSubject>> childrenByParent = scoped.stream()
                .filter(s -> s.getParentId() != null && byId.containsKey(s.getParentId()))
                .collect(Collectors.groupingBy(AccountSubject::getParentId));

        Map<Long, SubjectBalance> result = new HashMap<>();
        for (AccountSubject root : topLevelSubjects(scoped)) {
            result.put(root.getId(), rollupOne(root, childrenByParent, own));
        }
        return result;
    }

    private SubjectBalance rollupOne(AccountSubject node, Map<Long, List<AccountSubject>> childrenByParent,
                                     Map<Long, SubjectBalance> own) {
        SubjectBalance merged = new SubjectBalance();
        SubjectBalance self = own.get(node.getId());
        if (self != null) {
            merged.endPeriod = self.endPeriod;
            merged.endDebitMinusCredit = self.endDebitMinusCredit;
            merged.beginDebitMinusCredit = self.beginDebitMinusCredit;
        }
        for (AccountSubject child : childrenByParent.getOrDefault(node.getId(), List.of())) {
            SubjectBalance childSb = rollupOne(child, childrenByParent, own);
            merged.endDebitMinusCredit = merged.endDebitMinusCredit.add(childSb.endDebitMinusCredit);
            merged.beginDebitMinusCredit = merged.beginDebitMinusCredit.add(childSb.beginDebitMinusCredit);
        }
        return merged;
    }

    private List<AccountSubject> filterByType(List<AccountSubject> subjects, int subjectType) {
        return subjects.stream()
                .filter(s -> s.getSubjectType() != null && s.getSubjectType() == subjectType)
                .collect(Collectors.toList());
    }

    /** 按固定项目行顺序互斥匹配科目并汇总（debitSide=true 时资产取借方净额） */
    private List<ReportItem> buildSideItems(String[][] lineDefs, List<AccountSubject> sideSubjects,
                                            Map<Long, SubjectBalance> balances, boolean debitSide) {
        List<ReportItem> items = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (String[] def : lineDefs) {
            BigDecimal begin = BigDecimal.ZERO;
            BigDecimal end = BigDecimal.ZERO;
            Long subjectId = null;
            String subjectCode = null;
            for (AccountSubject s : sideSubjects) {
                if (used.contains(s.getId())) continue;
                String code = s.getSubjectCode() != null ? s.getSubjectCode() : "";
                if (!def[0].isEmpty() && !code.startsWith(def[0])) continue;
                used.add(s.getId());
                SubjectBalance sb = balances.get(s.getId());
                if (sb != null) {
                    begin = begin.add(toReportValue(sb.beginDebitMinusCredit, debitSide));
                    end = end.add(toReportValue(sb.endDebitMinusCredit, debitSide));
                }
                if (subjectId == null) {
                    subjectId = s.getId();
                    subjectCode = code;
                }
            }
            items.add(new ReportItem(def[1], begin, end, false, subjectId, subjectCode));
        }
        return items;
    }

    /** 所有者权益侧：41 开头权益科目 + 全部损益类科目计入未分配利润（未结转也保证平衡），其余权益科目计入期初资本 */
    private List<ReportItem> buildEquityItems(List<AccountSubject> equities, List<AccountSubject> profits,
                                              Map<Long, SubjectBalance> balances) {
        BigDecimal profitBegin = BigDecimal.ZERO;
        BigDecimal profitEnd = BigDecimal.ZERO;
        Long profitSubjectId = null;
        String profitSubjectCode = null;

        BigDecimal capitalBegin = BigDecimal.ZERO;
        BigDecimal capitalEnd = BigDecimal.ZERO;
        Long capitalSubjectId = null;
        String capitalSubjectCode = null;

        for (AccountSubject s : equities) {
            SubjectBalance sb = balances.get(s.getId());
            String code = s.getSubjectCode() != null ? s.getSubjectCode() : "";
            boolean retained = code.startsWith(RETAINED_EARNINGS_PREFIX);
            if (retained) {
                if (sb != null) {
                    profitBegin = profitBegin.add(sb.beginDebitMinusCredit.negate());
                    profitEnd = profitEnd.add(sb.endDebitMinusCredit.negate());
                }
                if (profitSubjectId == null) {
                    profitSubjectId = s.getId();
                    profitSubjectCode = code;
                }
            } else {
                if (sb != null) {
                    capitalBegin = capitalBegin.add(sb.beginDebitMinusCredit.negate());
                    capitalEnd = capitalEnd.add(sb.endDebitMinusCredit.negate());
                }
                if (capitalSubjectId == null) {
                    capitalSubjectId = s.getId();
                    capitalSubjectCode = code;
                }
            }
        }

        for (AccountSubject s : profits) {
            SubjectBalance sb = balances.get(s.getId());
            if (sb == null) continue;
            profitBegin = profitBegin.add(sb.beginDebitMinusCredit.negate());
            profitEnd = profitEnd.add(sb.endDebitMinusCredit.negate());
        }

        List<ReportItem> items = new ArrayList<>();
        items.add(new ReportItem(UNDISTRIBUTED_PROFIT_NAME, profitBegin, profitEnd, false,
                profitSubjectId, profitSubjectCode));
        items.add(new ReportItem(CAPITAL_NAME, capitalBegin, capitalEnd, false,
                capitalSubjectId, capitalSubjectCode));
        return items;
    }

    /** 资产取「借方净额」（贷方净额取反），负债/权益取「贷方净额」 */
    private BigDecimal toReportValue(BigDecimal debitMinusCredit, boolean debitSide) {
        BigDecimal value = debitMinusCredit != null ? debitMinusCredit : BigDecimal.ZERO;
        return debitSide ? value : value.negate();
    }

    private BigDecimal sumItems(List<ReportItem> items, boolean begin) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReportItem item : items) {
            total = total.add(begin ? item.begin : item.end);
        }
        return total;
    }

    /** 「显示为0科目」未勾选时隐藏余额全为 0 的明细行，合计行始终保留 */
    private List<ReportItem> filterZeroItems(List<ReportItem> items, boolean showZero) {
        if (showZero) return items;
        return items.stream()
                .filter(it -> it.totalRow
                        || it.end.compareTo(BigDecimal.ZERO) != 0
                        || it.begin.compareTo(BigDecimal.ZERO) != 0)
                .collect(Collectors.toList());
    }

    /** 左右按行号对齐合并为对照行 */
    private List<BalanceSheetRowDTO> mergeRows(List<ReportItem> left, List<ReportItem> right) {
        List<BalanceSheetRowDTO> rows = new ArrayList<>();
        int max = Math.max(left.size(), right.size());
        for (int i = 0; i < max; i++) {
            BalanceSheetRowDTO row = new BalanceSheetRowDTO();
            row.setRowNo(i + 1);
            if (i < left.size()) {
                ReportItem it = left.get(i);
                row.setAssetItemName(it.name);
                row.setAssetBeginBalance(it.begin);
                row.setAssetEndBalance(it.end);
                row.setAssetTotalRow(it.totalRow);
                row.setAssetSubjectId(it.subjectId);
                row.setAssetSubjectCode(it.subjectCode);
            }
            if (i < right.size()) {
                ReportItem it = right.get(i);
                row.setLiabilityItemName(it.name);
                row.setLiabilityBeginBalance(it.begin);
                row.setLiabilityEndBalance(it.end);
                row.setLiabilityTotalRow(it.totalRow);
                row.setLiabilitySubjectId(it.subjectId);
                row.setLiabilitySubjectCode(it.subjectCode);
            }
            rows.add(row);
        }
        return rows;
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** 单个科目的期末/年初余额（贷方为正口径） */
    private static class SubjectBalance {
        private Integer endPeriod;
        private BigDecimal endDebitMinusCredit = BigDecimal.ZERO;
        private Integer beginPeriod;
        private BigDecimal beginDebitMinusCredit = BigDecimal.ZERO;
    }

    /** 报表项目行（含合计行） */
    private static class ReportItem {
        private final String name;
        private final BigDecimal begin;
        private final BigDecimal end;
        private final boolean totalRow;
        private final Long subjectId;
        private final String subjectCode;

        private ReportItem(String name, BigDecimal begin, BigDecimal end, boolean totalRow,
                           Long subjectId, String subjectCode) {
            this.name = name;
            this.begin = begin;
            this.end = end;
            this.totalRow = totalRow;
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
        }
    }

    // ======== 利润表（科目层级驱动 · 金标准） ========

    private static final String SUMMARY_REVENUE_CODE = "SUMMARY_REVENUE";
    private static final String SUMMARY_OPERATING_PROFIT_CODE = "SUMMARY_OPERATING_PROFIT";
    private static final String SUMMARY_TOTAL_PROFIT_CODE = "SUMMARY_TOTAL_PROFIT";
    private static final String SUMMARY_NET_PROFIT_CODE = "SUMMARY_NET_PROFIT";

    private static final String REVENUE_ROW_NAME = "一、营业收入";
    private static final String OPERATING_PROFIT_ROW_NAME = "二、营业利润(亏损以'-'填列)";
    private static final String TOTAL_PROFIT_ROW_NAME = "三、利润总额(亏损总额以'-'填列)";
    private static final String NET_PROFIT_ROW_NAME = "四、净利润(净亏损以'-'填列)";

    private static final String PREFIX_ADD = "加：";
    private static final String PREFIX_SUBTRACT = "减：";

    /** 「一、营业收入」的取数科目（主营业务收入）；缺省时取编码最小的收入类科目 */
    private static final String PRIMARY_INCOME_SUBJECT_CODE = "6001";
    /** 营业外收入 / 营业外支出 / 所得税费用 的科目前缀 */
    private static final String NON_OPERATING_INCOME_PREFIX = "6301";
    private static final String NON_OPERATING_EXPENSE_PREFIX = "671";
    private static final String INCOME_TAX_PREFIX = "6801";

    @Override
    public IncomeStatementReportDTO generateIncomeStatementReport(IncomeStatementQuery query) {
        int fiscalYear = query.getFiscalYear() != null
                ? query.getFiscalYear() : java.time.LocalDate.now().getYear();
        int endPeriod = query.resolveEndPeriod();
        int startPeriod = query.resolveStartPeriod();
        int subjectLevel = query.getSubjectLevel() != null && query.getSubjectLevel() > 0
                ? query.getSubjectLevel() : 2;
        boolean showZero = !Boolean.FALSE.equals(query.getShowZero());

        // ① 数据源：只读取科目余额（finance_ledger，由凭证记账生成），不绕过凭证直改
        List<LedgerEntry> yearEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear);

        // ② 报表参与科目：启用的成本类(4)/损益类(5)，且层级 ≤ 科目层级
        List<AccountSubject> scoped = accountSubjectMapper.selectList(null).stream()
                .filter(s -> s.getSubjectType() != null)
                .filter(s -> s.getSubjectType() == 4 || s.getSubjectType() == 5)
                .filter(s -> s.getDeletedFlag() == null || s.getDeletedFlag() == 0)
                .filter(s -> s.getIsEnabled() == null || Boolean.TRUE.equals(s.getIsEnabled()))
                .filter(s -> s.getLevel() == null || s.getLevel() <= subjectLevel)
                .collect(Collectors.toList());

        Map<Long, AccountSubject> subjectById = scoped.stream()
                .collect(Collectors.toMap(AccountSubject::getId, s -> s, (a, b) -> a));
        Map<Long, List<AccountSubject>> childrenByParent = scoped.stream()
                .filter(s -> s.getParentId() != null && subjectById.containsKey(s.getParentId()))
                .collect(Collectors.groupingBy(AccountSubject::getParentId));
        // 顶级：无父科目，或父科目不在层级范围内（避免父子重复计算）
        List<AccountSubject> roots = scoped.stream()
                .filter(s -> s.getParentId() == null || !subjectById.containsKey(s.getParentId()))
                .collect(Collectors.toList());

        // ③ 科目自有发生额 → 按科目正常余额方向取正数（借方正/贷方正）
        Map<Long, BigDecimal[]> ownAmounts = new HashMap<>();
        for (LedgerEntry e : yearEntries) {
            if (e.getSubjectId() == null || e.getFiscalPeriod() == null) continue;
            AccountSubject subject = subjectById.get(e.getSubjectId());
            if (subject == null) continue;
            int period = e.getFiscalPeriod();
            boolean debitSide = subject.getDirection() == null || subject.getDirection() == 1;
            BigDecimal net = nz(e.getPeriodDebit()).subtract(nz(e.getPeriodCredit()));
            BigDecimal value = debitSide ? net : net.negate();

            BigDecimal[] bucket = ownAmounts.computeIfAbsent(e.getSubjectId(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (period >= startPeriod && period <= endPeriod) {
                bucket[0] = bucket[0].add(value);
            }
            if (period <= endPeriod) {
                bucket[1] = bucket[1].add(value);
            }
        }

        // ④ 父科目 = 自有发生额 + 全部子科目发生额（ql361：销售费用 = 其费用类型子项合计）
        Map<Long, BigDecimal[]> amounts = new HashMap<>();
        for (AccountSubject root : roots) {
            rollupSubjectAmount(root, childrenByParent, ownAmounts, amounts);
        }

        // ⑤ 按科目性质分段
        List<AccountSubject> costRoots = new ArrayList<>();
        List<AccountSubject> incomeRoots = new ArrayList<>();
        List<AccountSubject> expenseRoots = new ArrayList<>();
        List<AccountSubject> nonOperatingIncomeRoots = new ArrayList<>();
        List<AccountSubject> nonOperatingExpenseRoots = new ArrayList<>();
        List<AccountSubject> incomeTaxRoots = new ArrayList<>();
        for (AccountSubject s : roots) {
            String code = s.getSubjectCode() != null ? s.getSubjectCode() : "";
            if (s.getSubjectType() == 4) {
                costRoots.add(s);
            } else if (s.getDirection() != null && s.getDirection() == 2) {
                if (code.startsWith(NON_OPERATING_INCOME_PREFIX)) {
                    nonOperatingIncomeRoots.add(s);
                } else {
                    incomeRoots.add(s);
                }
            } else if (code.startsWith(INCOME_TAX_PREFIX)) {
                incomeTaxRoots.add(s);
            } else if (code.startsWith(NON_OPERATING_EXPENSE_PREFIX)) {
                nonOperatingExpenseRoots.add(s);
            } else {
                expenseRoots.add(s);
            }
        }

        // ⑥「一、营业收入」取主营业务收入（缺省取编码最小的收入类科目）
        AccountSubject primaryIncome = incomeRoots.stream()
                .filter(s -> PRIMARY_INCOME_SUBJECT_CODE.equals(s.getSubjectCode()))
                .findFirst()
                .orElseGet(() -> incomeRoots.stream()
                        .min(Comparator.comparing(s -> s.getSubjectCode() != null ? s.getSubjectCode() : "",
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElse(null));
        List<AccountSubject> otherIncomeRoots = incomeRoots.stream()
                .filter(s -> primaryIncome == null || !s.getId().equals(primaryIncome.getId()))
                .collect(Collectors.toList());

        BigDecimal[] revenue = amountOf(primaryIncome, amounts);
        BigDecimal[] otherIncome = sumRoots(otherIncomeRoots, amounts);
        BigDecimal[] cost = sumRoots(costRoots, amounts);
        BigDecimal[] expense = sumRoots(expenseRoots, amounts);
        BigDecimal[] nonOpIncome = sumRoots(nonOperatingIncomeRoots, amounts);
        BigDecimal[] nonOpExpense = sumRoots(nonOperatingExpenseRoots, amounts);
        BigDecimal[] incomeTax = sumRoots(incomeTaxRoots, amounts);

        // ⑦ 组装报表行（rowNo 统一在最后编号）
        List<IncomeStatementRowDTO> rows = new ArrayList<>();
        rows.add(summaryRow(SUMMARY_REVENUE_CODE, REVENUE_ROW_NAME, revenue, primaryIncome));
        appendSubjectRows(rows, costRoots, childrenByParent, amounts, PREFIX_SUBTRACT, showZero, 0);
        appendSubjectRows(rows, otherIncomeRoots, childrenByParent, amounts, PREFIX_ADD, showZero, 0);
        appendSubjectRows(rows, expenseRoots, childrenByParent, amounts, null, showZero, 0);

        BigDecimal[] operatingProfit = {
                revenue[0].add(otherIncome[0]).subtract(cost[0]).subtract(expense[0]),
                revenue[1].add(otherIncome[1]).subtract(cost[1]).subtract(expense[1])};
        rows.add(summaryRow(SUMMARY_OPERATING_PROFIT_CODE, OPERATING_PROFIT_ROW_NAME, operatingProfit, null));

        appendSubjectRows(rows, nonOperatingIncomeRoots, childrenByParent, amounts, PREFIX_ADD, showZero, 0);
        appendSubjectRows(rows, nonOperatingExpenseRoots, childrenByParent, amounts, PREFIX_SUBTRACT, showZero, 0);

        BigDecimal[] totalProfit = {
                operatingProfit[0].add(nonOpIncome[0]).subtract(nonOpExpense[0]),
                operatingProfit[1].add(nonOpIncome[1]).subtract(nonOpExpense[1])};
        rows.add(summaryRow(SUMMARY_TOTAL_PROFIT_CODE, TOTAL_PROFIT_ROW_NAME, totalProfit, null));

        appendSubjectRows(rows, incomeTaxRoots, childrenByParent, amounts, PREFIX_SUBTRACT, showZero, 0);

        BigDecimal[] netProfit = {
                totalProfit[0].subtract(incomeTax[0]),
                totalProfit[1].subtract(incomeTax[1])};
        rows.add(summaryRow(SUMMARY_NET_PROFIT_CODE, NET_PROFIT_ROW_NAME, netProfit, null));

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRowNo(i + 1);
        }

        // ⑧ 组装结果
        IncomeStatementReportDTO report = new IncomeStatementReportDTO();
        report.setFiscalYear(fiscalYear);
        report.setFiscalPeriod(endPeriod);
        report.setPeriodMode(query.isMultiPeriod() ? "multi" : "single");
        report.setStartPeriod(startPeriod);
        report.setEndPeriod(endPeriod);
        report.setSubjectLevel(subjectLevel);
        report.setShowZero(showZero);
        report.setRows(rows);

        report.setRevenueTotal(revenue[0]);
        report.setOtherIncomeTotal(otherIncome[0]);
        report.setCostTotal(cost[0]);
        report.setExpenseTotal(expense[0]);
        report.setOperatingProfit(operatingProfit[0]);
        report.setNonOperatingIncomeTotal(nonOpIncome[0]);
        report.setNonOperatingExpenseTotal(nonOpExpense[0]);
        report.setTotalProfit(totalProfit[0]);
        report.setIncomeTaxTotal(incomeTax[0]);
        report.setNetProfit(netProfit[0]);

        report.setRevenueTotalCumulative(revenue[1]);
        report.setOtherIncomeTotalCumulative(otherIncome[1]);
        report.setCostTotalCumulative(cost[1]);
        report.setExpenseTotalCumulative(expense[1]);
        report.setOperatingProfitCumulative(operatingProfit[1]);
        report.setNonOperatingIncomeTotalCumulative(nonOpIncome[1]);
        report.setNonOperatingExpenseTotalCumulative(nonOpExpense[1]);
        report.setTotalProfitCumulative(totalProfit[1]);
        report.setIncomeTaxTotalCumulative(incomeTax[1]);
        report.setNetProfitCumulative(netProfit[1]);
        report.setHasData(!yearEntries.isEmpty());

        log.info("生成利润表: year={}, 期间={}~{}, 层级={}, 显示为0={}, 行数={}, 营业利润={}, 净利润={}",
                fiscalYear, startPeriod, endPeriod, subjectLevel, showZero, rows.size(),
                operatingProfit[0], netProfit[0]);
        return report;
    }

    /** 递归汇总：父科目本期/累计 = 自有发生额 + 全部子科目发生额 */
    private BigDecimal[] rollupSubjectAmount(AccountSubject subject,
                                             Map<Long, List<AccountSubject>> childrenByParent,
                                             Map<Long, BigDecimal[]> ownAmounts,
                                             Map<Long, BigDecimal[]> target) {
        BigDecimal[] cached = target.get(subject.getId());
        if (cached != null) return cached;

        BigDecimal[] own = ownAmounts.getOrDefault(subject.getId(),
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal current = own[0];
        BigDecimal cumulative = own[1];

        List<AccountSubject> children = childrenByParent.get(subject.getId());
        if (children != null) {
            for (AccountSubject child : children) {
                BigDecimal[] childAmount = rollupSubjectAmount(child, childrenByParent, ownAmounts, target);
                current = current.add(childAmount[0]);
                cumulative = cumulative.add(childAmount[1]);
            }
        }
        BigDecimal[] result = {current, cumulative};
        target.put(subject.getId(), result);
        return result;
    }

    private BigDecimal[] amountOf(AccountSubject subject, Map<Long, BigDecimal[]> amounts) {
        if (subject == null) return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal[] amount = amounts.get(subject.getId());
        return amount != null ? amount : new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private BigDecimal[] sumRoots(List<AccountSubject> roots, Map<Long, BigDecimal[]> amounts) {
        BigDecimal current = BigDecimal.ZERO;
        BigDecimal cumulative = BigDecimal.ZERO;
        for (AccountSubject s : roots) {
            BigDecimal[] amount = amountOf(s, amounts);
            current = current.add(amount[0]);
            cumulative = cumulative.add(amount[1]);
        }
        return new BigDecimal[]{current, cumulative};
    }

    private IncomeStatementRowDTO summaryRow(String itemCode, String itemName, BigDecimal[] amount,
                                             AccountSubject subject) {
        IncomeStatementRowDTO row = new IncomeStatementRowDTO();
        row.setItemCode(itemCode);
        row.setItemName(itemName);
        row.setIndentLevel(0);
        row.setSummaryRow(Boolean.TRUE);
        row.setSubjectLevel(0);
        row.setCurrentAmount(amount[0]);
        row.setCumulativeAmount(amount[1]);
        if (subject != null) {
            row.setSubjectId(subject.getId());
            row.setSubjectCode(subject.getSubjectCode());
        }
        return row;
    }

    /** 按科目树追加明细行：一级科目带 加：/减： 前缀，子科目按层级缩进且不再带前缀 */
    private void appendSubjectRows(List<IncomeStatementRowDTO> out, List<AccountSubject> roots,
                                   Map<Long, List<AccountSubject>> childrenByParent,
                                   Map<Long, BigDecimal[]> amounts, String prefix,
                                   boolean showZero, int depth) {
        if (roots == null || roots.isEmpty()) return;
        List<AccountSubject> sorted = roots.stream()
                .sorted(Comparator.comparing(s -> s.getSubjectCode() != null ? s.getSubjectCode() : "",
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        for (AccountSubject subject : sorted) {
            BigDecimal[] amount = amountOf(subject, amounts);
            boolean zero = amount[0].compareTo(BigDecimal.ZERO) == 0
                    && amount[1].compareTo(BigDecimal.ZERO) == 0;
            if (!zero || showZero) {
                IncomeStatementRowDTO row = new IncomeStatementRowDTO();
                row.setItemCode(subject.getSubjectCode());
                row.setSubjectCode(subject.getSubjectCode());
                row.setItemName((depth == 0 && prefix != null ? prefix : "")
                        + (subject.getSubjectName() != null ? subject.getSubjectName() : ""));
                row.setIndentLevel(depth);
                row.setSummaryRow(Boolean.FALSE);
                row.setSubjectId(subject.getId());
                row.setSubjectLevel(subject.getLevel());
                row.setCurrentAmount(amount[0]);
                row.setCumulativeAmount(amount[1]);
                out.add(row);
            }
            appendSubjectRows(out, childrenByParent.get(subject.getId()), childrenByParent,
                    amounts, null, showZero, depth + 1);
        }
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

        // 所得税费用（6801）
        //
        // ⚠️ 2026-09-23 修复：此前 `totalTax` 声明后**从未累加**，而净利润按
        //   `grossProfit - totalExpense - totalTax` 计算 ⇒ 所得税恒为 0、净利润虚高
        //   （见 FINANCE_MODULE_AUDIT §2.4）。此处补上所得税类科目的累加。
        //   当前库中尚无 6801 数据，故修复后该项仍可能为 0 —— 那是「数据未录入」，
        //   而非此前的「代码写死」；财务启用该科目后报表即自动反映。
        String[] taxCodes = {"6801"};
        for (String code : taxCodes) {
            IncomeStatementDTO item = buildIncomeItem(yearEntries, allSubjects, code,
                    startMonth, endMonth, "expense");
            if (item != null) {
                result.add(item);
                totalTax = totalTax.add(item.getCurrentAmount() != null ? item.getCurrentAmount() : BigDecimal.ZERO);
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
