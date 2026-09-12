package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.TrialBalanceAggDTO;
import cn.aiedge.erp.finance.dto.TrialBalancePageDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceQuery;
import cn.aiedge.erp.finance.dto.TrialBalanceRowDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceSummaryDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.TrialBalanceMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.service.TrialBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 科目余额表 Service 实现
 *
 * 口径（P0 红线）：只读取自凭证分录，仅取已记账凭证；期初余额 = 年初结转 + 会月起之前本年净额；
 * 期末余额 = 期初 + 本期借 − 本期贷。父级科目金额 = 本级 + 全部下级汇总，合计行只累加「顶级科目」，
 * 避免父子重复累加，保证表级 Σ借 = Σ贷 试算平衡。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrialBalanceServiceImpl implements TrialBalanceService {

    /** 科目类型名称（与 AccountSubject.subjectType 字典一致） */
    private static final Map<Integer, String> SUBJECT_TYPE_NAMES = Map.of(
            1, "资产类", 2, "负债类", 3, "权益类", 4, "成本类", 5, "损益类");

    private final TrialBalanceMapper trialBalanceMapper;
    private final AccountSubjectMapper accountSubjectMapper;

    @Override
    public TrialBalancePageDTO queryTrialBalance(TrialBalanceQuery query) {
        TrialBalanceQuery q = normalize(query);

        // 1. 凭证分录聚合（本期/本年/会月起之前净额）+ 年初结转净额
        Map<String, TrialBalanceAggDTO> ownByCode = loadAggregation(q);

        // 2. 科目主数据（启用中的科目，按编码升序）
        List<AccountSubject> subjects = accountSubjectMapper.selectList(null).stream()
                .filter(s -> s.getDeletedFlag() == null || s.getDeletedFlag() == 0)
                .filter(s -> !Boolean.FALSE.equals(s.getIsEnabled()))
                .filter(s -> StringUtils.hasText(s.getSubjectCode()))
                .sorted(Comparator.comparing(AccountSubject::getSubjectCode))
                .collect(Collectors.toList());

        Map<Long, List<AccountSubject>> childrenByParent = subjects.stream()
                .filter(s -> s.getParentId() != null)
                .collect(Collectors.groupingBy(AccountSubject::getParentId));
        Map<Long, AccountSubject> byId = subjects.stream()
                .collect(Collectors.toMap(AccountSubject::getId, s -> s, (a, b) -> a));

        // 3. 子树汇总（本级 + 全部下级）
        Map<Long, TrialBalanceAggDTO> rolled = new HashMap<>();
        for (AccountSubject subject : subjects) {
            rollup(subject, childrenByParent, ownByCode, rolled, new HashSet<>());
        }

        // 4. 过滤：科目层级 → 科目关键字（含下级）→ 显示无数据科目
        Set<Long> displayed = resolveDisplayed(subjects, byId, childrenByParent, rolled, q);

        // 5. 组装科目行
        List<TrialBalanceRowDTO> records = new ArrayList<>();
        for (AccountSubject subject : subjects) {
            if (!displayed.contains(subject.getId())) {
                continue;
            }
            records.add(toRow(subject, rolled.get(subject.getId()), childrenByParent));
        }

        // 6. 合计行：只累加「父级未出现在结果集」的顶级科目，避免父子重复累加
        TrialBalanceSummaryDTO summary = buildSummary(subjects, displayed, rolled);

        TrialBalancePageDTO result = new TrialBalancePageDTO();
        result.setRecords(records);
        result.setTotal(records.size());
        result.setSummary(summary);

        log.info("科目余额表: year={}, period={}-{}, level={}, keyword={}, 行数={}, 本期平衡={}",
                q.getFiscalYear(), q.getStartPeriod(), q.getEndPeriod(), q.getSubjectLevel(),
                q.getSubjectKeyword(), records.size(), summary.getPeriodBalanced());
        return result;
    }

    // ═══════════════════════════════════════════
    // 取数
    // ═══════════════════════════════════════════

    /** 凭证分录聚合 + 会计年首期期初（年初结转），按科目编码合并 */
    private Map<String, TrialBalanceAggDTO> loadAggregation(TrialBalanceQuery q) {
        Map<String, TrialBalanceAggDTO> map = new LinkedHashMap<>();
        for (TrialBalanceAggDTO agg : trialBalanceMapper.aggregateBySubject(q)) {
            if (agg.getSubjectCode() == null) {
                continue;
            }
            agg.setOpeningNet(nvl(agg.getOpeningNet()));
            agg.setPeriodDebit(nvl(agg.getPeriodDebit()));
            agg.setPeriodCredit(nvl(agg.getPeriodCredit()));
            agg.setYearDebit(nvl(agg.getYearDebit()));
            agg.setYearCredit(nvl(agg.getYearCredit()));
            map.put(agg.getSubjectCode(), agg);
        }
        for (TrialBalanceAggDTO opening : trialBalanceMapper.selectYearOpening(q.getFiscalYear())) {
            if (opening.getSubjectCode() == null || nvl(opening.getOpeningNet()).compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            TrialBalanceAggDTO target = map.computeIfAbsent(opening.getSubjectCode(), code -> emptyAgg(code));
            target.setOpeningNet(nvl(target.getOpeningNet()).add(nvl(opening.getOpeningNet())));
        }
        return map;
    }

    /** 递归汇总：本级 + 全部下级（带环保护） */
    private TrialBalanceAggDTO rollup(AccountSubject subject,
                                      Map<Long, List<AccountSubject>> childrenByParent,
                                      Map<String, TrialBalanceAggDTO> ownByCode,
                                      Map<Long, TrialBalanceAggDTO> memo,
                                      Set<Long> visiting) {
        TrialBalanceAggDTO cached = memo.get(subject.getId());
        if (cached != null) {
            return cached;
        }
        TrialBalanceAggDTO acc = copyOf(ownByCode.get(subject.getSubjectCode()), subject.getSubjectCode());
        if (visiting.add(subject.getId())) {
            for (AccountSubject child : childrenByParent.getOrDefault(subject.getId(), List.of())) {
                TrialBalanceAggDTO childAgg = rollup(child, childrenByParent, ownByCode, memo, visiting);
                acc.setOpeningNet(nvl(acc.getOpeningNet()).add(nvl(childAgg.getOpeningNet())));
                acc.setPeriodDebit(nvl(acc.getPeriodDebit()).add(nvl(childAgg.getPeriodDebit())));
                acc.setPeriodCredit(nvl(acc.getPeriodCredit()).add(nvl(childAgg.getPeriodCredit())));
                acc.setYearDebit(nvl(acc.getYearDebit()).add(nvl(childAgg.getYearDebit())));
                acc.setYearCredit(nvl(acc.getYearCredit()).add(nvl(childAgg.getYearCredit())));
            }
            visiting.remove(subject.getId());
        }
        memo.put(subject.getId(), acc);
        return acc;
    }

    // ═══════════════════════════════════════════
    // 过滤
    // ═══════════════════════════════════════════

    private Set<Long> resolveDisplayed(List<AccountSubject> subjects,
                                       Map<Long, AccountSubject> byId,
                                       Map<Long, List<AccountSubject>> childrenByParent,
                                       Map<Long, TrialBalanceAggDTO> rolled,
                                       TrialBalanceQuery q) {
        Integer maxLevel = q.getSubjectLevel() != null && q.getSubjectLevel() > 0 ? q.getSubjectLevel() : null;
        String keyword = StringUtils.hasText(q.getSubjectKeyword()) ? q.getSubjectKeyword().trim().toLowerCase() : null;

        // 科目关键字命中的科目 + 其全部下级（父级仅作为层级上下文，未命中且非父级上下文时不显示）
        Set<Long> keywordScope = null;
        if (keyword != null) {
            keywordScope = new HashSet<>();
            for (AccountSubject subject : subjects) {
                boolean hit = subject.getSubjectCode().toLowerCase().contains(keyword)
                        || (subject.getSubjectName() != null && subject.getSubjectName().toLowerCase().contains(keyword));
                if (hit) {
                    collectSubtree(subject, childrenByParent, keywordScope);
                }
            }
        }

        Set<Long> displayed = new HashSet<>();
        for (AccountSubject subject : subjects) {
            if (maxLevel != null && (subject.getLevel() == null || subject.getLevel() > maxLevel)) {
                continue;
            }
            if (keywordScope != null && !keywordScope.contains(subject.getId())) {
                continue;
            }
            if (!Boolean.TRUE.equals(q.getShowNoData()) && isEmpty(rolled.get(subject.getId()))) {
                continue;
            }
            displayed.add(subject.getId());
        }
        // 科目关键字的命中项若因层级/无数据被过滤，仍保证命中科目本身可见（口径可对账）
        if (keywordScope != null) {
            for (Long id : keywordScope) {
                AccountSubject subject = byId.get(id);
                if (subject == null) {
                    continue;
                }
                boolean hitSelf = subject.getSubjectCode().toLowerCase().contains(keyword)
                        || (subject.getSubjectName() != null && subject.getSubjectName().toLowerCase().contains(keyword));
                if (hitSelf && (maxLevel == null || (subject.getLevel() != null && subject.getLevel() <= maxLevel))) {
                    displayed.add(id);
                }
            }
        }
        return displayed;
    }

    private void collectSubtree(AccountSubject root,
                                Map<Long, List<AccountSubject>> childrenByParent,
                                Set<Long> target) {
        Deque<AccountSubject> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            AccountSubject current = stack.pop();
            if (!target.add(current.getId())) {
                continue;
            }
            for (AccountSubject child : childrenByParent.getOrDefault(current.getId(), List.of())) {
                stack.push(child);
            }
        }
    }

    private boolean isEmpty(TrialBalanceAggDTO agg) {
        if (agg == null) {
            return true;
        }
        return nvl(agg.getOpeningNet()).compareTo(BigDecimal.ZERO) == 0
                && nvl(agg.getPeriodDebit()).compareTo(BigDecimal.ZERO) == 0
                && nvl(agg.getPeriodCredit()).compareTo(BigDecimal.ZERO) == 0
                && nvl(agg.getYearDebit()).compareTo(BigDecimal.ZERO) == 0
                && nvl(agg.getYearCredit()).compareTo(BigDecimal.ZERO) == 0;
    }

    // ═══════════════════════════════════════════
    // 组装
    // ═══════════════════════════════════════════

    private TrialBalanceRowDTO toRow(AccountSubject subject,
                                     TrialBalanceAggDTO agg,
                                     Map<Long, List<AccountSubject>> childrenByParent) {
        BigDecimal opening = nvl(agg != null ? agg.getOpeningNet() : null);
        BigDecimal periodDebit = nvl(agg != null ? agg.getPeriodDebit() : null);
        BigDecimal periodCredit = nvl(agg != null ? agg.getPeriodCredit() : null);
        BigDecimal closing = opening.add(periodDebit).subtract(periodCredit);

        TrialBalanceRowDTO row = new TrialBalanceRowDTO();
        row.setSubjectType(subject.getSubjectType());
        row.setSubjectTypeName(SUBJECT_TYPE_NAMES.getOrDefault(subject.getSubjectType(), "其他"));
        row.setSubjectCode(subject.getSubjectCode());
        row.setSubjectName(subject.getSubjectName());
        row.setLevel(subject.getLevel());
        row.setOpeningBalance(opening);
        row.setOpeningDirection(directionOf(opening));
        row.setPeriodDebit(periodDebit);
        row.setPeriodCredit(periodCredit);
        row.setYearDebit(nvl(agg != null ? agg.getYearDebit() : null));
        row.setYearCredit(nvl(agg != null ? agg.getYearCredit() : null));
        row.setClosingBalance(closing);
        row.setClosingDirection(directionOf(closing));
        row.setHasChildren(!childrenByParent.getOrDefault(subject.getId(), List.of()).isEmpty());
        return row;
    }

    private TrialBalanceSummaryDTO buildSummary(List<AccountSubject> subjects,
                                                Set<Long> displayed,
                                                Map<Long, TrialBalanceAggDTO> rolled) {
        BigDecimal openingDebit = BigDecimal.ZERO;
        BigDecimal openingCredit = BigDecimal.ZERO;
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;
        BigDecimal yearDebit = BigDecimal.ZERO;
        BigDecimal yearCredit = BigDecimal.ZERO;
        BigDecimal closingDebit = BigDecimal.ZERO;
        BigDecimal closingCredit = BigDecimal.ZERO;

        for (AccountSubject subject : subjects) {
            if (!displayed.contains(subject.getId())) {
                continue;
            }
            // 只累加顶级科：父级未出现在结果集中（父级也在结果集时其金额已含本行，避免重复累加）
            if (subject.getParentId() != null && displayed.contains(subject.getParentId())) {
                continue;
            }
            TrialBalanceAggDTO agg = rolled.get(subject.getId());
            BigDecimal opening = nvl(agg != null ? agg.getOpeningNet() : null);
            BigDecimal pDebit = nvl(agg != null ? agg.getPeriodDebit() : null);
            BigDecimal pCredit = nvl(agg != null ? agg.getPeriodCredit() : null);
            BigDecimal closing = opening.add(pDebit).subtract(pCredit);

            openingDebit = openingDebit.add(positive(opening));
            openingCredit = openingCredit.add(negative(opening));
            closingDebit = closingDebit.add(positive(closing));
            closingCredit = closingCredit.add(negative(closing));
            periodDebit = periodDebit.add(pDebit);
            periodCredit = periodCredit.add(pCredit);
            yearDebit = yearDebit.add(nvl(agg != null ? agg.getYearDebit() : null));
            yearCredit = yearCredit.add(nvl(agg != null ? agg.getYearCredit() : null));
        }

        TrialBalanceSummaryDTO summary = new TrialBalanceSummaryDTO();
        summary.setOpeningDebit(openingDebit);
        summary.setOpeningCredit(openingCredit);
        summary.setPeriodDebit(periodDebit);
        summary.setPeriodCredit(periodCredit);
        summary.setYearDebit(yearDebit);
        summary.setYearCredit(yearCredit);
        summary.setClosingDebit(closingDebit);
        summary.setClosingCredit(closingCredit);
        summary.setPeriodBalanced(periodDebit.compareTo(periodCredit) == 0);
        summary.setYearBalanced(yearDebit.compareTo(yearCredit) == 0);
        return summary;
    }

    // ═══════════════════════════════════════════
    // 工具
    // ═══════════════════════════════════════════

    private TrialBalanceQuery normalize(TrialBalanceQuery query) {
        TrialBalanceQuery q = query != null ? query : new TrialBalanceQuery();
        LocalDate today = LocalDate.now();
        if (q.getFiscalYear() == null) {
            q.setFiscalYear(today.getYear());
        }
        if (q.getEndPeriod() == null || q.getEndPeriod() < 1 || q.getEndPeriod() > 12) {
            q.setEndPeriod(today.getMonthValue());
        }
        if (q.getStartPeriod() == null || q.getStartPeriod() < 1 || q.getStartPeriod() > 12) {
            q.setStartPeriod(q.getEndPeriod());
        }
        if (q.getStartPeriod() > q.getEndPeriod()) {
            int tmp = q.getStartPeriod();
            q.setStartPeriod(q.getEndPeriod());
            q.setEndPeriod(tmp);
        }
        if (q.getShowNoData() == null) {
            q.setShowNoData(false);
        }
        return q;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String directionOf(BigDecimal balance) {
        return balance != null && balance.compareTo(BigDecimal.ZERO) < 0 ? "贷" : "借";
    }

    private static BigDecimal positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    private static BigDecimal negative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0 ? value.abs() : BigDecimal.ZERO;
    }

    private static TrialBalanceAggDTO emptyAgg(String subjectCode) {
        TrialBalanceAggDTO agg = new TrialBalanceAggDTO();
        agg.setSubjectCode(subjectCode);
        agg.setOpeningNet(BigDecimal.ZERO);
        agg.setPeriodDebit(BigDecimal.ZERO);
        agg.setPeriodCredit(BigDecimal.ZERO);
        agg.setYearDebit(BigDecimal.ZERO);
        agg.setYearCredit(BigDecimal.ZERO);
        return agg;
    }

    private static TrialBalanceAggDTO copyOf(TrialBalanceAggDTO source, String subjectCode) {
        if (source == null) {
            return emptyAgg(subjectCode);
        }
        TrialBalanceAggDTO copy = emptyAgg(subjectCode);
        copy.setOpeningNet(nvl(source.getOpeningNet()));
        copy.setPeriodDebit(nvl(source.getPeriodDebit()));
        copy.setPeriodCredit(nvl(source.getPeriodCredit()));
        copy.setYearDebit(nvl(source.getYearDebit()));
        copy.setYearCredit(nvl(source.getYearCredit()));
        return copy;
    }
}
