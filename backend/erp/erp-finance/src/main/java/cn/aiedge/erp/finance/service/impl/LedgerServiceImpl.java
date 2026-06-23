package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.LedgerEntryDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.LedgerEntryMapper;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import cn.aiedge.erp.finance.model.entity.Voucher;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.LedgerService;
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
