package cn.aiedge.erp.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.GeneralLedgerQueryDTO;
import cn.aiedge.erp.finance.dto.GeneralLedgerRowDTO;
import cn.aiedge.erp.finance.dto.LedgerDetailDTO;
import cn.aiedge.erp.finance.dto.LedgerDetailPageDTO;
import cn.aiedge.erp.finance.dto.LedgerDetailQuery;
import cn.aiedge.erp.finance.dto.LedgerDetailRowDTO;
import cn.aiedge.erp.finance.dto.LedgerSubjectTreeNodeDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.LedgerDetailMapper;
import cn.aiedge.erp.finance.mapper.VoucherItemMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.VoucherItem;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import cn.aiedge.erp.finance.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** 科目类型名称（与 AccountSubject.subjectType 字典一致） */
    private static final Map<Integer, String> SUBJECT_TYPE_NAMES = Map.of(
            1, "资产类", 2, "负债类", 3, "权益类", 4, "成本类", 5, "损益类");

    private final LedgerService ledgerService;
    private final VoucherItemMapper voucherItemMapper;
    private final LedgerDetailMapper ledgerDetailMapper;
    private final AccountSubjectMapper accountSubjectMapper;
    private final AccountSubjectService accountSubjectService;

    @Operation(summary = "总账（按科目层级汇总：期初余额 + 本期发生 = 期末余额）")
    @GetMapping("/general")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询总账")
    public Result<List<GeneralLedgerRowDTO>> general(GeneralLedgerQueryDTO query) {
        return Result.success(ledgerService.queryGeneralReport(query));
    }

    @Operation(summary = "总账科目层级选项")
    @GetMapping("/levels")
    @SaCheckPermission("finance:report:view")
    public Result<List<Integer>> levels() {
        return Result.success(ledgerService.listSubjectLevels());
    }

    @Operation(summary = "明细账（按科目逐笔凭证分录）")
    @GetMapping("/detail")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询明细账")
    public Result<List<LedgerDetailDTO>> detail(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "科目编码（精确匹配）") @RequestParam(required = false) String subjectCode) {
        return Result.success(queryDetail(fiscalYear, fiscalPeriod, subjectCode));
    }

    @Operation(summary = "明细账分页查询（期初余额 + 逐笔发生额与期末余额 + 合计）")
    @GetMapping("/detail-page")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询明细账")
    public Result<LedgerDetailPageDTO> detailPage(LedgerDetailQuery query) {
        return Result.success(queryDetailPage(query != null ? query : new LedgerDetailQuery()));
    }

    @Operation(summary = "明细账科目分类树（全部 / 资产类 / 负债类 / 权益类 / 成本类 / 损益类）")
    @GetMapping("/subject-tree")
    @SaCheckPermission("finance:report:view")
    @OperationLog(module = "分类账", type = "QUERY", desc = "查询明细账科目分类树")
    public Result<List<LedgerSubjectTreeNodeDTO>> subjectTree() {
        return Result.success(buildSubjectTree());
    }

    // ═══════════════════════════════════════════
    // 内部实现
    // ═══════════════════════════════════════════

    /**
     * 明细账分页查询。
     * 余额口径：期初 + Σ借 − Σ贷，逐笔累计，逐笔可追溯（P0 账簿口径红线）。
     */
    private LedgerDetailPageDTO queryDetailPage(LedgerDetailQuery query) {
        // 1. 科目范围：选中具体科目时展开为「本级 + 全部下级」科目编码
        resolveSubjectScope(query);

        // 2. 期初余额：同口径过滤条件下、起始日之前的净额（未指定起始日则从零起算）
        BigDecimal opening = BigDecimal.ZERO;
        if (StringUtils.hasText(query.getDateStart())) {
            BigDecimal net = ledgerDetailMapper.sumLedgerNetBefore(query, query.getDateStart());
            opening = net != null ? net : BigDecimal.ZERO;
        }

        // 3. 逐笔分录（同口径过滤 + 日期区间）
        List<LedgerDetailRowDTO> entries = ledgerDetailMapper.selectLedgerRows(query);

        // 4. 逐笔累计期末余额
        BigDecimal running = opening;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (LedgerDetailRowDTO row : entries) {
            BigDecimal debit = nvl(row.getDebitAmount());
            BigDecimal credit = nvl(row.getCreditAmount());
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            running = running.add(debit).subtract(credit);
            row.setRowType("ENTRY");
            row.setBalance(running);
            row.setBalanceDirection(directionOf(running));
        }
        BigDecimal closing = running;

        // 5. 组装「期初余额」首行 + 内存分页（余额需全局累计，故先取全量再切片）
        List<LedgerDetailRowDTO> full = new ArrayList<>(entries.size() + 1);
        full.add(buildOpeningRow(opening));
        full.addAll(entries);

        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 20;
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int from = Math.min((pageNum - 1) * pageSize, full.size());
        int to = Math.min(from + pageSize, full.size());

        LedgerDetailPageDTO result = new LedgerDetailPageDTO();
        result.setRecords(new ArrayList<>(full.subList(from, to)));
        result.setTotal(full.size());
        result.setOpeningBalance(opening);
        result.setOpeningDirection(directionOf(opening));
        result.setTotalDebit(totalDebit);
        result.setTotalCredit(totalCredit);
        result.setClosingBalance(closing);
        result.setClosingDirection(directionOf(closing));
        return result;
    }

    private LedgerDetailRowDTO buildOpeningRow(BigDecimal opening) {
        LedgerDetailRowDTO row = new LedgerDetailRowDTO();
        row.setRowType("OPENING");
        row.setVoucherNo("期初余额");
        row.setSummary("期初余额");
        row.setDebitAmount(BigDecimal.ZERO);
        row.setCreditAmount(BigDecimal.ZERO);
        row.setBalance(opening);
        row.setBalanceDirection(directionOf(opening));
        return row;
    }

    /** 科目树选中单科目时，展开为「本级 + 全部下级」科目编码集合 */
    private void resolveSubjectScope(LedgerDetailQuery query) {
        if (query.getSubjectId() == null) {
            return;
        }
        query.setSubjectCodes(accountSubjectService.collectCodeWithDescendants(query.getSubjectId()));
    }

    /** 科目分类树：全部 / 资产类 / 负债类 / 权益类 / 成本类 / 损益类，组内按科目层级展开 */
    private List<LedgerSubjectTreeNodeDTO> buildSubjectTree() {
        LedgerSubjectTreeNodeDTO all = new LedgerSubjectTreeNodeDTO();
        all.setId("0");
        all.setCategoryName("全部");

        List<AccountSubject> subjects = accountSubjectMapper.selectList(null).stream()
                .filter(s -> !Boolean.FALSE.equals(s.getIsEnabled()))
                .filter(s -> StringUtils.hasText(s.getSubjectCode()))
                .sorted(Comparator.comparing(AccountSubject::getSubjectCode))
                .collect(Collectors.toList());

        List<LedgerSubjectTreeNodeDTO> nodes = new ArrayList<>();
        nodes.add(all);

        Map<Long, LedgerSubjectTreeNodeDTO> nodeById = new LinkedHashMap<>();
        for (AccountSubject subject : subjects) {
            nodeById.put(subject.getId(), toTreeNode(subject));
        }

        for (Integer type : List.of(1, 2, 3, 4, 5)) {
            List<AccountSubject> group = subjects.stream()
                    .filter(s -> type.equals(s.getSubjectType()))
                    .collect(Collectors.toList());
            if (group.isEmpty()) {
                continue;
            }
            LedgerSubjectTreeNodeDTO typeNode = new LedgerSubjectTreeNodeDTO();
            typeNode.setId("type_" + type);
            typeNode.setCategoryName(SUBJECT_TYPE_NAMES.getOrDefault(type, "其他"));
            typeNode.setSubjectType(type);

            for (AccountSubject subject : group) {
                LedgerSubjectTreeNodeDTO node = nodeById.get(subject.getId());
                if (subject.getParentId() != null && nodeById.containsKey(subject.getParentId())) {
                    nodeById.get(subject.getParentId()).getChildren().add(node);
                } else {
                    typeNode.getChildren().add(node);
                }
            }
            nodes.add(typeNode);
        }
        return nodes;
    }

    private LedgerSubjectTreeNodeDTO toTreeNode(AccountSubject subject) {
        LedgerSubjectTreeNodeDTO node = new LedgerSubjectTreeNodeDTO();
        node.setId(String.valueOf(subject.getId()));
        node.setCategoryName(subject.getSubjectCode() + " " + subject.getSubjectName());
        node.setSubjectId(subject.getId());
        node.setSubjectType(subject.getSubjectType());
        return node;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String directionOf(BigDecimal balance) {
        return balance != null && balance.compareTo(BigDecimal.ZERO) < 0 ? "贷" : "借";
    }

    private List<LedgerDetailDTO> queryDetail(Integer fiscalYear, Integer fiscalPeriod, String subjectCode) {
        List<VoucherItem> items = voucherItemMapper.findLedgerDetail(fiscalYear, fiscalPeriod, subjectCode);
        return items.stream().map(this::toDetailDTO).collect(Collectors.toList());
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
