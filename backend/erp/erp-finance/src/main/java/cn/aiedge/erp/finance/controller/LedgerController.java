package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.LedgerEntryDTO;
import cn.aiedge.erp.finance.dto.LedgerDetailDTO;
import cn.aiedge.erp.finance.mapper.LedgerEntryMapper;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类账Controller：总账 + 明细账
 */
@Tag(name = "分类账", description = "总账、明细账查询接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerEntryMapper ledgerEntryMapper;
    private final VoucherItemMapper voucherItemMapper;

    @Operation(summary = "总账（按科目汇总期初/本期/期末借贷）")
    @GetMapping("/general")
    @PreAuthorize("hasPermission('/api/erp/finance/ledger/general', 'finance:report:view')")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询总账")
    public Result<List<LedgerEntryDTO>> general(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam(required = false) Integer fiscalPeriod) {
        return Result.success(queryGeneral(fiscalYear, fiscalPeriod));
    }

    @Operation(summary = "明细账（按科目逐笔凭证分录）")
    @GetMapping("/detail")
    @PreAuthorize("hasPermission('/api/erp/finance/ledger/detail', 'finance:report:view')")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询明细账")
    public Result<List<LedgerDetailDTO>> detail(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "科目编码（精确匹配）") @RequestParam(required = false) String subjectCode) {
        return Result.success(queryDetail(fiscalYear, fiscalPeriod, subjectCode));
    }

    // ═══════════════════════════════════════════
    // 内部实现
    // ═══════════════════════════════════════════

    private List<LedgerEntryDTO> queryGeneral(Integer fiscalYear, Integer fiscalPeriod) {
        List<LedgerEntry> entries;
        if (fiscalYear != null) {
            entries = ledgerEntryMapper.findByFiscalYear(fiscalYear);
            if (fiscalPeriod != null) {
                entries = entries.stream()
                        .filter(e -> e.getFiscalPeriod() != null && e.getFiscalPeriod().intValue() == fiscalPeriod)
                        .collect(Collectors.toList());
            }
        } else {
            entries = ledgerEntryMapper.selectList(null);
        }
        return entries.stream()
                .sorted(Comparator
                        .comparing((LedgerEntry e) -> e.getSubjectCode() == null ? "" : e.getSubjectCode())
                        .thenComparing(LedgerEntry::getFiscalPeriod))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private List<LedgerDetailDTO> queryDetail(Integer fiscalYear, Integer fiscalPeriod, String subjectCode) {
        List<VoucherItem> items = voucherItemMapper.findLedgerDetail(fiscalYear, fiscalPeriod, subjectCode);
        return items.stream().map(this::toDetailDTO).collect(Collectors.toList());
    }

    private LedgerEntryDTO toDTO(LedgerEntry entry) {
        LedgerEntryDTO dto = new LedgerEntryDTO();
        dto.setId(entry.getId());
        dto.setSubjectId(entry.getSubjectId());
        dto.setSubjectCode(entry.getSubjectCode());
        dto.setSubjectName(entry.getSubjectName());
        dto.setFiscalYear(entry.getFiscalYear());
        dto.setFiscalPeriod(entry.getFiscalPeriod());
        dto.setOpeningDebit(entry.getOpeningDebit());
        dto.setOpeningCredit(entry.getOpeningCredit());
        dto.setPeriodDebit(entry.getPeriodDebit());
        dto.setPeriodCredit(entry.getPeriodCredit());
        dto.setClosingDebit(entry.getClosingDebit());
        dto.setClosingCredit(entry.getClosingCredit());
        dto.setClosingBalance(entry.getClosingBalance());
        dto.setBalanceDirection(entry.getBalanceDirection());
        return dto;
    }

    private LedgerDetailDTO toDetailDTO(VoucherItem item) {
        LedgerDetailDTO dto = new LedgerDetailDTO();
        dto.setId(item.getId());
        dto.setVoucherId(item.getVoucherId());
        dto.setVoucherNo(item.getVoucherNo());
        dto.setVoucherDate(item.getVoucherDate());
        dto.setSummary(item.getSummary());
        dto.setSubjectId(item.getSubjectId());
        dto.setSubjectCode(item.getSubjectCode());
        dto.setSubjectName(item.getSubjectName());
        dto.setDebitAmount(item.getDebitAmount());
        dto.setCreditAmount(item.getCreditAmount());
        dto.setSourceType(item.getSourceType());
        dto.setSourceNo(item.getSourceNo());
        return dto;
    }
}
