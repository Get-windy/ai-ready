package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.GeneralLedgerQueryDTO;
import cn.aiedge.erp.finance.dto.GeneralLedgerRowDTO;
import cn.aiedge.erp.finance.dto.LedgerEntryDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.LedgerEntryMapper;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import cn.aiedge.erp.finance.model.entity.Voucher;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 分类账Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryMapper ledgerEntryMapper;
    private final AccountSubjectMapper accountSubjectMapper;
    private final VoucherItemMapper voucherItemMapper;

    @Override
    public List<LedgerEntryDTO> getLedger(Long subjectId, Integer fiscalYear) {
        List<LedgerEntry> entries = ledgerEntryMapper.findBySubjectIdInAndFiscalYear(
                List.of(subjectId), fiscalYear);
        return entries.stream()
                .sorted(Comparator.comparingInt(LedgerEntry::getFiscalPeriod))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTrialBalance(Integer fiscalYear, Integer fiscalPeriod) {
        List<LedgerEntry> allEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear).stream()
                .filter(e -> e.getFiscalPeriod().intValue() == fiscalPeriod.intValue())
                .collect(Collectors.toList());

        List<TrialBalanceDTO> trialItems = allEntries.stream()
                .map(this::toTrialBalanceDTO)
                .sorted(Comparator.comparing(
                        dto -> dto.getSubjectCode() != null ? dto.getSubjectCode() : ""))
                .collect(Collectors.toList());

        // 计算合计数
        BigDecimal totalOpeningDebit = allEntries.stream()
                .map(e -> e.getOpeningDebit() != null ? e.getOpeningDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpeningCredit = allEntries.stream()
                .map(e -> e.getOpeningCredit() != null ? e.getOpeningCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPeriodDebit = allEntries.stream()
                .map(e -> e.getPeriodDebit() != null ? e.getPeriodDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPeriodCredit = allEntries.stream()
                .map(e -> e.getPeriodCredit() != null ? e.getPeriodCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalClosingDebit = allEntries.stream()
                .map(e -> e.getClosingDebit() != null ? e.getClosingDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalClosingCredit = allEntries.stream()
                .map(e -> e.getClosingCredit() != null ? e.getClosingCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean balanced = totalOpeningDebit.compareTo(totalOpeningCredit) == 0
                && totalPeriodDebit.compareTo(totalPeriodCredit) == 0
                && totalClosingDebit.compareTo(totalClosingCredit) == 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", trialItems);
        result.put("totalOpeningDebit", totalOpeningDebit);
        result.put("totalOpeningCredit", totalOpeningCredit);
        result.put("totalPeriodDebit", totalPeriodDebit);
        result.put("totalPeriodCredit", totalPeriodCredit);
        result.put("totalClosingDebit", totalClosingDebit);
        result.put("totalClosingCredit", totalClosingCredit);
        result.put("isBalanced", balanced);

        return result;
    }

    @Override
    public List<Integer> listSubjectLevels() {
        return accountSubjectMapper.selectList(null).stream()
                .map(AccountSubject::getLevel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<GeneralLedgerRowDTO> queryGeneralReport(GeneralLedgerQueryDTO query) {
        // ═══ 1. 会计月起止（缺省取当前会计月） ═══
        int[] start = parsePeriod(query.getPeriodStart());
        int[] end = parsePeriod(query.getPeriodEnd());
        LocalDate today = LocalDate.now();
        if (start == null && end == null) {
            start = new int[]{today.getYear(), today.getMonthValue()};
            end = new int[]{start[0], start[1]};
        } else if (start == null) {
            start = new int[]{end[0], end[1]};
        } else if (end == null) {
            end = new int[]{start[0], start[1]};
        }
        if (periodKey(start[0], start[1]) > periodKey(end[0], end[1])) {
            int[] tmp = start;
            start = end;
            end = tmp;
        }
        int startKey = periodKey(start[0], start[1]);
        int endKey = periodKey(end[0], end[1]);
        int level = query.getSubjectLevel() != null && query.getSubjectLevel() > 0 ? query.getSubjectLevel() : 1;
        boolean hideNoAmount = Boolean.TRUE.equals(query.getHideNoAmount());
        String keyword = query.getSubjectCode() == null ? "" : query.getSubjectCode().trim();

        // ═══ 2. 科目字典（层级上溯用） ═══
        List<AccountSubject> subjects = accountSubjectMapper.selectList(null);
        Map<Long, AccountSubject> subjectById = subjects.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(AccountSubject::getId, s -> s, (a, b) -> a));
        Map<String, AccountSubject> subjectByCode = subjects.stream()
                .filter(s -> s.getSubjectCode() != null)
                .collect(Collectors.toMap(AccountSubject::getSubjectCode, s -> s, (a, b) -> a));

        // ═══ 3. 按科目汇总账簿数据 ═══
        Map<String, List<LedgerEntry>> entriesByCode = new LinkedHashMap<>();
        for (LedgerEntry entry : ledgerEntryMapper.selectList(null)) {
            if (entry.getSubjectCode() == null) {
                continue;
            }
            entriesByCode.computeIfAbsent(entry.getSubjectCode(), k -> new ArrayList<>()).add(entry);
        }

        Map<String, SubjectAgg> aggByDisplayCode = new LinkedHashMap<>();
        for (Map.Entry<String, List<LedgerEntry>> group : entriesByCode.entrySet()) {
            List<LedgerEntry> entries = group.getValue();
            entries.sort(Comparator
                    .comparingInt((LedgerEntry e) -> e.getFiscalYear() == null ? 0 : e.getFiscalYear())
                    .thenComparingInt(e -> e.getFiscalPeriod() == null ? 0 : e.getFiscalPeriod()));

            AccountSubject leaf = subjectByCode.get(group.getKey());
            AccountSubject display = resolveDisplaySubject(leaf, subjectById, level);
            String displayCode = display != null && display.getSubjectCode() != null
                    ? display.getSubjectCode() : group.getKey();

            SubjectAgg agg = aggByDisplayCode.computeIfAbsent(displayCode, k -> {
                SubjectAgg created = new SubjectAgg();
                created.subjectCode = k;
                created.subjectName = display != null && display.getSubjectName() != null
                        ? display.getSubjectName() : entries.get(0).getSubjectName();
                created.subjectId = display != null ? display.getId() : entries.get(0).getSubjectId();
                created.level = display != null ? display.getLevel() : null;
                return created;
            });

            // 期初余额（带符号：借正贷负）= 账簿初始期初 + 起始期间之前各期发生净额
            agg.opening = agg.opening.add(signedOpeningBefore(entries, startKey));

            // 本年初余额（end 所在年度 1 月之前）
            agg.yearOpening = agg.yearOpening.add(signedOpeningBefore(entries, periodKey(end[0], 1)));

            // 区间内各期发生额
            for (LedgerEntry entry : entries) {
                int key = periodKey(entry.getFiscalYear(), entry.getFiscalPeriod());
                BigDecimal debit = nz(entry.getPeriodDebit());
                BigDecimal credit = nz(entry.getPeriodCredit());
                if (key >= startKey && key <= endKey) {
                    BigDecimal[] amounts = agg.periods.computeIfAbsent(key,
                            k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    amounts[0] = amounts[0].add(debit);
                    amounts[1] = amounts[1].add(credit);
                    agg.periodDebit = agg.periodDebit.add(debit);
                    agg.periodCredit = agg.periodCredit.add(credit);
                }
                // 本年累计：end 年度 1 月起至会计月(止)
                if (entry.getFiscalYear() != null && entry.getFiscalYear() == end[0]
                        && entry.getFiscalPeriod() != null && entry.getFiscalPeriod() <= end[1]) {
                    agg.yearDebit = agg.yearDebit.add(debit);
                    agg.yearCredit = agg.yearCredit.add(credit);
                }
            }
        }

        // ═══ 4. 生成账簿行 ═══
        boolean singlePeriod = startKey == endKey;
        boolean showYearAccum = Boolean.TRUE.equals(query.getShowYearAccum());
        List<SubjectAgg> ordered = aggByDisplayCode.values().stream()
                .filter(a -> keyword.isEmpty()
                        || (a.subjectCode != null && a.subjectCode.contains(keyword))
                        || (a.subjectName != null && a.subjectName.contains(keyword)))
                .sorted(Comparator.comparing(a -> a.subjectCode == null ? "" : a.subjectCode))
                .collect(Collectors.toList());

        List<GeneralLedgerRowDTO> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalClosing = BigDecimal.ZERO;

        for (SubjectAgg agg : ordered) {
            rows.add(buildRow(agg, periodCode(startKey), "期初余额", null, null, agg.opening, "opening"));

            BigDecimal running = agg.opening;
            for (int key = startKey; key <= endKey; key = nextPeriodKey(key)) {
                BigDecimal[] amounts = agg.periods.get(key);
                BigDecimal debit = amounts == null ? BigDecimal.ZERO : amounts[0];
                BigDecimal credit = amounts == null ? BigDecimal.ZERO : amounts[1];
                if (hideNoAmount && debit.signum() == 0 && credit.signum() == 0) {
                    continue;
                }
                running = running.add(debit).subtract(credit);
                rows.add(buildRow(agg, periodCode(key), singlePeriod ? "本期合计" : "本期发生",
                        debit, credit, running, "period"));
            }

            if (showYearAccum) {
                BigDecimal yearBalance = agg.yearOpening.add(agg.yearDebit).subtract(agg.yearCredit);
                rows.add(buildRow(agg, periodCode(endKey), "本年累计",
                        agg.yearDebit, agg.yearCredit, yearBalance, "yearTotal"));
            }

            totalDebit = totalDebit.add(agg.periodDebit);
            totalCredit = totalCredit.add(agg.periodCredit);
            totalClosing = totalClosing.add(agg.opening.add(agg.periodDebit).subtract(agg.periodCredit));
        }

        if (Boolean.TRUE.equals(query.getShowCurrentTotal())) {
            GeneralLedgerRowDTO totalRow = new GeneralLedgerRowDTO();
            totalRow.setSubjectCode("");
            totalRow.setSubjectName("");
            totalRow.setPeriod(periodCode(endKey));
            totalRow.setSummary("当前总计");
            totalRow.setDebit(totalDebit);
            totalRow.setCredit(totalCredit);
            totalRow.setDirection(directionOf(totalClosing));
            totalRow.setBalance(totalClosing.abs());
            totalRow.setRowType("grandTotal");
            totalRow.setDrillable(false);
            rows.add(totalRow);
        }

        return rows;
    }

    /**
     * 指定期间之前的期初余额（带符号：借正贷负）= 账簿初始期初 + 该期间之前各期发生净额
     */
    private BigDecimal signedOpeningBefore(List<LedgerEntry> entries, int beforeKey) {
        if (entries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        LedgerEntry first = entries.get(0);
        return nz(first.getOpeningDebit()).subtract(nz(first.getOpeningCredit()))
                .add(accBefore(entries, beforeKey));
    }

    /**
     * 指定期间之前（不含）各期发生净额合计
     */
    private BigDecimal accBefore(List<LedgerEntry> entries, int beforeKey) {
        BigDecimal acc = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            int key = periodKey(entry.getFiscalYear(), entry.getFiscalPeriod());
            if (key < beforeKey) {
                acc = acc.add(nz(entry.getPeriodDebit())).subtract(nz(entry.getPeriodCredit()));
            }
        }
        return acc;
    }

    /**
     * 把明细科目上溯到第 level 级科目（用于科目层级汇总）
     */
    private AccountSubject resolveDisplaySubject(AccountSubject subject, Map<Long, AccountSubject> byId, int level) {
        if (subject == null) {
            return null;
        }
        AccountSubject current = subject;
        int guard = 0;
        while (current.getLevel() != null && current.getLevel() > level
                && current.getParentId() != null && guard++ < 10) {
            AccountSubject parent = byId.get(current.getParentId());
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private GeneralLedgerRowDTO buildRow(SubjectAgg agg, String period, String summary,
                                         BigDecimal debit, BigDecimal credit, BigDecimal signedBalance,
                                         String rowType) {
        GeneralLedgerRowDTO row = new GeneralLedgerRowDTO();
        row.setSubjectId(agg.subjectId);
        row.setSubjectCode(agg.subjectCode);
        row.setSubjectName(agg.subjectName);
        row.setPeriod(period);
        row.setSummary(summary);
        row.setDebit(debit);
        row.setCredit(credit);
        row.setDirection(directionOf(signedBalance));
        row.setBalance(signedBalance.abs());
        row.setRowType(rowType);
        row.setLevel(agg.level);
        row.setDrillable(agg.subjectCode != null && !agg.subjectCode.isEmpty());
        return row;
    }

    private String directionOf(BigDecimal signedBalance) {
        int cmp = signedBalance == null ? 0 : signedBalance.signum();
        if (cmp > 0) {
            return "借";
        }
        if (cmp < 0) {
            return "贷";
        }
        return "平";
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** "YYYY-MM" / "YYYYMM" → [年, 月] */
    private int[] parsePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            return null;
        }
        String text = period.trim().replace("-", "");
        if (text.length() < 6) {
            return null;
        }
        try {
            int year = Integer.parseInt(text.substring(0, 4));
            int month = Integer.parseInt(text.substring(4, 6));
            if (month < 1 || month > 12) {
                return null;
            }
            return new int[]{year, month};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int periodKey(Integer year, Integer period) {
        return (year == null ? 0 : year) * 100 + (period == null ? 0 : period);
    }

    /** 期间键递增一月 */
    private int nextPeriodKey(int key) {
        int year = key / 100;
        int month = key % 100;
        return month >= 12 ? (year + 1) * 100 + 1 : key + 1;
    }

    /** 期间键 → "YYYY-MM" */
    private String periodCode(int key) {
        return String.format("%04d-%02d", key / 100, key % 100);
    }

    /** 总账聚合中间结构 */
    private static class SubjectAgg {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private Integer level;
        /** 期初余额（借正贷负） */
        private BigDecimal opening = BigDecimal.ZERO;
        /** 本年初余额（借正贷负） */
        private BigDecimal yearOpening = BigDecimal.ZERO;
        /** 区间内借/贷发生合计 */
        private BigDecimal periodDebit = BigDecimal.ZERO;
        private BigDecimal periodCredit = BigDecimal.ZERO;
        /** 本年累计借/贷 */
        private BigDecimal yearDebit = BigDecimal.ZERO;
        private BigDecimal yearCredit = BigDecimal.ZERO;
        /** 期间键 → [借方, 贷方] */
        private final TreeMap<Integer, BigDecimal[]> periods = new TreeMap<>();
    }

    @Override
    @Transactional
    public void postToLedger(Voucher voucher) {
        List<VoucherItem> items = voucher.getItems();
        if (items == null || items.isEmpty()) {
            items = voucherItemMapper.findByVoucherId(voucher.getId());
        }

        for (VoucherItem item : items) {
            Long subjectId = item.getSubjectId();
            Integer fiscalYear = voucher.getFiscalYear();
            Integer fiscalPeriod = voucher.getFiscalPeriod();

            // 查找或创建分类账条目
            LedgerEntry entry = ledgerEntryMapper
                    .findBySubjectIdAndFiscalYearAndFiscalPeriod(subjectId, fiscalYear, fiscalPeriod)
                    .orElseGet(() -> {
                        LedgerEntry newEntry = new LedgerEntry();
                        newEntry.setSubjectId(subjectId);
                        newEntry.setSubjectCode(item.getSubjectCode());
                        newEntry.setSubjectName(item.getSubjectName());
                        newEntry.setFiscalYear(fiscalYear);
                        newEntry.setFiscalPeriod(fiscalPeriod);
                        newEntry.setOpeningDebit(BigDecimal.ZERO);
                        newEntry.setOpeningCredit(BigDecimal.ZERO);
                        newEntry.setPeriodDebit(BigDecimal.ZERO);
                        newEntry.setPeriodCredit(BigDecimal.ZERO);

                        // 从上一期结转期初余额
                        if (fiscalPeriod > 1) {
                            ledgerEntryMapper
                                    .findBySubjectIdAndFiscalYearAndFiscalPeriod(subjectId, fiscalYear, fiscalPeriod - 1)
                                    .ifPresent(prev -> {
                                        newEntry.setOpeningDebit(
                                                prev.getClosingDebit() != null ? prev.getClosingDebit() : BigDecimal.ZERO);
                                        newEntry.setOpeningCredit(
                                                prev.getClosingCredit() != null ? prev.getClosingCredit() : BigDecimal.ZERO);
                                    });
                        }
                        return newEntry;
                    });

            // 累加本期发生额
            BigDecimal debit = item.getDebitAmount() != null ? item.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO;
            entry.setPeriodDebit(
                    (entry.getPeriodDebit() != null ? entry.getPeriodDebit() : BigDecimal.ZERO).add(debit));
            entry.setPeriodCredit(
                    (entry.getPeriodCredit() != null ? entry.getPeriodCredit() : BigDecimal.ZERO).add(credit));

            // 计算期末余额
            BigDecimal openingBalance = (entry.getOpeningDebit() != null ? entry.getOpeningDebit() : BigDecimal.ZERO)
                    .subtract(entry.getOpeningCredit() != null ? entry.getOpeningCredit() : BigDecimal.ZERO);
            BigDecimal netChange = (entry.getPeriodDebit() != null ? entry.getPeriodDebit() : BigDecimal.ZERO)
                    .subtract(entry.getPeriodCredit() != null ? entry.getPeriodCredit() : BigDecimal.ZERO);
            BigDecimal closingBalance = openingBalance.add(netChange);

            if (closingBalance.compareTo(BigDecimal.ZERO) >= 0) {
                entry.setClosingDebit(closingBalance);
                entry.setClosingCredit(BigDecimal.ZERO);
            } else {
                entry.setClosingDebit(BigDecimal.ZERO);
                entry.setClosingCredit(closingBalance.abs());
            }
            entry.setClosingBalance(closingBalance);
            entry.setBalanceDirection(closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? 1 : 2);

            if (entry.getId() != null) {
                ledgerEntryMapper.updateById(entry);
            } else {
                ledgerEntryMapper.insert(entry);
            }
        }

        log.info("过账到分类账: voucherNo={}, itemsCount={}", voucher.getVoucherNo(), items.size());
    }

    @Override
    @Transactional
    public void closePeriod(Integer fiscalYear, Integer fiscalPeriod) {
        List<LedgerEntry> currentEntries = ledgerEntryMapper.findByFiscalYear(fiscalYear).stream()
                .filter(e -> e.getFiscalPeriod().intValue() == fiscalPeriod.intValue())
                .collect(Collectors.toList());

        if (currentEntries.isEmpty()) {
            log.warn("期间无分类账数据可结账: year={}, period={}", fiscalYear, fiscalPeriod);
            return;
        }

        // 计算下一期间
        int nextPeriod = fiscalPeriod + 1;
        int nextYear = fiscalYear;
        if (nextPeriod > 12) {
            nextPeriod = 1;
            nextYear = fiscalYear + 1;
        }

        int finalNextYear = nextYear;
        int finalNextPeriod = nextPeriod;

        for (LedgerEntry entry : currentEntries) {
            // 检查下一期间是否已有记录
            ledgerEntryMapper
                    .findBySubjectIdAndFiscalYearAndFiscalPeriod(entry.getSubjectId(), finalNextYear, finalNextPeriod)
                    .ifPresentOrElse(nextEntry -> {
                        // 更新已有记录的期初余额
                        nextEntry.setOpeningDebit(entry.getClosingDebit() != null ? entry.getClosingDebit() : BigDecimal.ZERO);
                        nextEntry.setOpeningCredit(entry.getClosingCredit() != null ? entry.getClosingCredit() : BigDecimal.ZERO);
                        ledgerEntryMapper.updateById(nextEntry);
                    }, () -> {
                        // 创建新的下一期间记录
                        LedgerEntry nextEntry = new LedgerEntry();
                        nextEntry.setSubjectId(entry.getSubjectId());
                        nextEntry.setSubjectCode(entry.getSubjectCode());
                        nextEntry.setSubjectName(entry.getSubjectName());
                        nextEntry.setFiscalYear(finalNextYear);
                        nextEntry.setFiscalPeriod(finalNextPeriod);
                        nextEntry.setOpeningDebit(
                                entry.getClosingDebit() != null ? entry.getClosingDebit() : BigDecimal.ZERO);
                        nextEntry.setOpeningCredit(
                                entry.getClosingCredit() != null ? entry.getClosingCredit() : BigDecimal.ZERO);
                        nextEntry.setPeriodDebit(BigDecimal.ZERO);
                        nextEntry.setPeriodCredit(BigDecimal.ZERO);
                        nextEntry.setClosingDebit(
                                entry.getClosingDebit() != null ? entry.getClosingDebit() : BigDecimal.ZERO);
                        nextEntry.setClosingCredit(
                                entry.getClosingCredit() != null ? entry.getClosingCredit() : BigDecimal.ZERO);
                        nextEntry.setClosingBalance(entry.getClosingBalance());
                        nextEntry.setBalanceDirection(entry.getBalanceDirection());
                        ledgerEntryMapper.insert(nextEntry);
                    });
        }

        log.info("期末结账完成: year={}, period={} -> year={}, period={}",
                fiscalYear, fiscalPeriod, finalNextYear, finalNextPeriod);
    }

    // ======== DTO转换 ========

    private LedgerEntryDTO toDTO(LedgerEntry entity) {
        LedgerEntryDTO dto = new LedgerEntryDTO();
        dto.setId(entity.getId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectCode(entity.getSubjectCode());
        dto.setSubjectName(entity.getSubjectName());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setFiscalPeriod(entity.getFiscalPeriod());
        dto.setOpeningDebit(entity.getOpeningDebit());
        dto.setOpeningCredit(entity.getOpeningCredit());
        dto.setPeriodDebit(entity.getPeriodDebit());
        dto.setPeriodCredit(entity.getPeriodCredit());
        dto.setClosingDebit(entity.getClosingDebit());
        dto.setClosingCredit(entity.getClosingCredit());
        dto.setClosingBalance(entity.getClosingBalance());
        dto.setBalanceDirection(entity.getBalanceDirection());
        return dto;
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
