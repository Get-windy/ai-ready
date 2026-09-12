package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.AuxBalancePageDTO;
import cn.aiedge.erp.finance.dto.AuxBalanceQuery;
import cn.aiedge.erp.finance.dto.AuxBalanceSummaryDTO;
import cn.aiedge.erp.finance.mapper.AuxBalanceMapper;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * 辅助核算余额Service实现类（辅助核算余额表）
 *
 * 取数完全来自凭证分录（finance_voucher_item ⨝ finance_voucher，仅 posted），
 * 按 科目 + 核算项 汇总四段余额：期初余额 / 本期发生额 / 本年累计 / 期末余额（各含借、贷）。
 *
 * 余额公式（每行与合计行均成立）：期初借 − 期初贷 + 本期借 − 本期贷 = 期末借 − 期末贷。
 * 勾稽：同一科目下各核算项的四段金额逐列加总 = 该科目在科目余额表中的对应金额。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAuxiliaryBalanceServiceImpl implements FinanceAuxiliaryBalanceService {

    /** 核算项类型白名单 */
    private static final Set<String> AUX_TYPES = Set.of("PARTNER", "CUSTOMER", "SUPPLIER", "EMPLOYEE", "DEPT");

    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 单次查询上限：兼容报表「导出」一次性拉取全量（前端传 pageSize=9999） */
    private static final int MAX_PAGE_SIZE = 10000;

    private final AuxBalanceMapper auxBalanceMapper;
    private final AccountSubjectService accountSubjectService;

    @Override
    public AuxBalancePageDTO page(AuxBalanceQuery query) {
        AuxBalanceQuery q = query != null ? query : new AuxBalanceQuery();
        normalize(q);

        AuxBalancePageDTO result = new AuxBalancePageDTO();
        long total = auxBalanceMapper.countRows(q);
        result.setTotal(total);
        if (total > 0) {
            result.setRecords(auxBalanceMapper.selectRows(q));
        }
        AuxBalanceSummaryDTO summary = auxBalanceMapper.selectSummary(q);
        result.setSummary(summary != null ? summary : new AuxBalanceSummaryDTO());
        return result;
    }

    /** 归一化查询条件：会计月区间、科目范围、核算项类型、分页参数 */
    private void normalize(AuxBalanceQuery q) {
        YearMonth end = parseMonth(q.getEndMonth(), "会计月(止)");
        if (end == null) {
            end = YearMonth.now();
        }
        YearMonth start = parseMonth(q.getStartMonth(), "会计月(起)");
        if (start == null) {
            start = end;
        }
        if (start.isAfter(end)) {
            throw BusinessException.badRequest("会计月(起)不能晚于会计月(止)");
        }
        q.setStartMonth(start.toString());
        q.setEndMonth(end.toString());
        q.setStartPeriodKey(start.getYear() * 100 + start.getMonthValue());
        q.setEndPeriodKey(end.getYear() * 100 + end.getMonthValue());
        q.setEndFiscalYear(end.getYear());
        q.setEndFiscalMonth(end.getMonthValue());

        // 科目范围：指定科目编码直接使用；指定科目ID展开为「本级 + 全部下级」
        if (StringUtils.hasText(q.getSubjectCode())) {
            q.setSubjectCodes(List.of(q.getSubjectCode().trim()));
        } else if (q.getSubjectId() != null) {
            q.setSubjectCodes(accountSubjectService.collectCodeWithDescendants(q.getSubjectId()));
        } else {
            q.setSubjectCodes(List.of());
        }

        String auxType = StringUtils.hasText(q.getAuxType()) ? q.getAuxType().trim().toUpperCase() : "PARTNER";
        q.setAuxType(AUX_TYPES.contains(auxType) ? auxType : "PARTNER");

        q.setHideNoPeriodAmount(Boolean.TRUE.equals(q.getHideNoPeriodAmount()));
        q.setHideZeroBalance(Boolean.TRUE.equals(q.getHideZeroBalance()));
        q.setPageNum(q.getPageNum() != null && q.getPageNum() > 0 ? q.getPageNum() : 1);
        int size = q.getPageSize() != null && q.getPageSize() > 0 ? q.getPageSize() : DEFAULT_PAGE_SIZE;
        q.setPageSize(Math.min(size, MAX_PAGE_SIZE));
    }

    /** 解析 yyyy-MM；为空返回 null（由调用方取默认值） */
    private YearMonth parseMonth(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest(label + "格式应为 yyyy-MM，实际为: " + value);
        }
    }
}
