package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.MonthClosingResultDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.mapper.AccountSubjectMapper;
import cn.aiedge.erp.finance.mapper.AccountingPeriodMapper;
import cn.aiedge.erp.finance.mapper.LedgerEntryMapper;
import cn.aiedge.erp.finance.mapper.MonthClosingLogMapper;
import cn.aiedge.erp.finance.mapper.VoucherMapper;
import cn.aiedge.erp.finance.model.entity.AccountSubject;
import cn.aiedge.erp.finance.model.entity.AccountingPeriod;
import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import cn.aiedge.erp.finance.model.entity.MonthClosingLog;
import cn.aiedge.erp.finance.model.entity.Voucher;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.MonthClosingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 月结Service实现类
 * <p>
 * 月结流程（对标金蝶/用友总账月结，P0/P1 红线）：
 * 1. 期间必须存在且处于开启状态
 * 2. 借贷平衡校验：期间内所有凭证借=贷（P1，存在未平衡凭证阻止关账）
 * 3. 该期间不得存在未过账凭证（finance_voucher.status 为 draft/audited；
 *    posted=已过账通过，reversed=已冲销不阻断）
 * 4. 期末结转损益：收入/费用转入本年利润 4103，生成结转凭证（KJPZ-，P1，不得直改科目余额）
 * 全部通过则关闭期间（status=0）并写入月结日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthClosingServiceImpl implements MonthClosingService {

    /** 无租户上下文时回落的租户（定时任务/初始化场景，与既有初始化数据一致） */
    private static final Long FALLBACK_TENANT_ID = 1L;

    /**
     * 本模块所属租户：会话上下文优先，取不到才回落。
     *
     * <p><b>2026-09-23 修复</b>：此前月结的期间查询/日志读写一律写死租户 1 ⇒
     * 非 1 租户月结直接抛「会计期间不存在」而不可用，月结日志还会落到租户 1（跨租户错写）。
     * 取法与 {@code AccountingPeriodServiceImpl#currentTenantId} 保持一致。</p>
     */
    private Long currentTenantId() {
        Long tid = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        return tid != null ? tid : FALLBACK_TENANT_ID;
    }

    /**
     * 本年利润科目编码（权益类 4103）
     */
    private static final String SUBJECT_YEAR_PROFIT = "4103";
    /**
     * 损益类科目类型：5-损益
     */
    private static final int SUBJECT_TYPE_PROFIT_LOSS = 5;
    private static final Pattern PERIOD_CODE_PATTERN = Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])$");
    /**
     * 未过账凭证明细快照上限
     */
    private static final int DETAIL_LIMIT = 20;

    private final AccountingPeriodMapper accountingPeriodMapper;
    private final MonthClosingLogMapper monthClosingLogMapper;
    private final VoucherMapper voucherMapper;
    private final AccountSubjectMapper accountSubjectMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final BusinessAccountingService businessAccountingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MonthClosingResultDTO execute(String periodCode, String operatorId, String operatorName) {
        int[] ym = parsePeriodCode(periodCode);
        AccountingPeriod period = getPeriodOrThrow(periodCode);
        if (period.getStatus() != null && period.getStatus() == 0) {
            throw BusinessException.badRequest("期间已关闭，无需重复月结: " + periodCode);
        }

        List<MonthClosingResultDTO.CheckItem> checks = runChecks(ym[0], ym[1]);
        boolean passed = checks.stream().allMatch(MonthClosingResultDTO.CheckItem::getPassed);

        MonthClosingResultDTO result = new MonthClosingResultDTO();
        result.setPeriodCode(periodCode);
        result.setChecks(checks);

        if (!passed) {
            result.setSuccess(false);
            result.setMessage("月结检查未通过，期间未关闭");
            log.warn("月结检查未通过: period={}, checks={}", periodCode, checks);
            return result;
        }

        // 期末结转损益：收入/费用转入本年利润 4103，生成 KJPZ- 结转凭证（P1）
        String carryOverVoucherNo = carryOverProfitLoss(ym[0], ym[1], period, periodCode);
        result.setCarryOverVoucherNo(carryOverVoucherNo);

        // 关闭期间
        LocalDateTime now = LocalDateTime.now();
        period.setStatus(0);
        period.setClosedBy(operatorName != null ? operatorName : operatorId);
        period.setClosedTime(now);
        accountingPeriodMapper.updateById(period);

        // 写月结日志（含检查结果快照 + 结转凭证号）
        writeLog(periodCode, "close", operatorId, operatorName, buildCheckResultJson(checks, true, carryOverVoucherNo));

        result.setSuccess(true);
        result.setMessage(carryOverVoucherNo != null
                ? "月结成功，期间已关闭，结转凭证: " + carryOverVoucherNo
                : "月结成功，期间已关闭（本期无可结转损益）");
        result.setClosedBy(period.getClosedBy());
        result.setClosedTime(now);
        log.info("月结成功: period={}, operator={}, carryOver={}", periodCode, operatorId, carryOverVoucherNo);
        return result;
    }

    @Override
    @Transactional
    public List<MonthClosingResultDTO> batchExecute(List<String> periodCodes, String operatorId, String operatorName) {
        List<MonthClosingResultDTO> results = new ArrayList<>();
        if (periodCodes == null || periodCodes.isEmpty()) {
            return results;
        }
        for (String code : periodCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                results.add(execute(code.trim(), operatorId, operatorName));
            } catch (Exception e) {
                log.error("批量月结单个期间失败: period={}, error={}", code, e.getMessage(), e);
                MonthClosingResultDTO r = new MonthClosingResultDTO();
                r.setPeriodCode(code.trim());
                r.setSuccess(false);
                r.setMessage("月结异常: " + e.getMessage());
                results.add(r);
            }
        }
        return results;
    }

    @Override
    @Transactional
    public MonthClosingResultDTO reopen(String periodCode, String operatorId, String operatorName) {
        parsePeriodCode(periodCode);
        AccountingPeriod period = getPeriodOrThrow(periodCode);
        if (period.getStatus() == null || period.getStatus() != 0) {
            throw BusinessException.badRequest("期间未关闭，无需反月结: " + periodCode);
        }

        period.setStatus(1);
        period.setClosedBy(null);
        period.setClosedTime(null);
        accountingPeriodMapper.updateById(period);

        writeLog(periodCode, "reopen", operatorId, operatorName, null);

        MonthClosingResultDTO result = new MonthClosingResultDTO();
        result.setPeriodCode(periodCode);
        result.setSuccess(true);
        result.setMessage("反月结成功，期间已重新开启");
        log.info("反月结成功: period={}, operator={}", periodCode, operatorId);
        return result;
    }

    @Override
    public Map<String, Object> status(String periodCode) {
        parsePeriodCode(periodCode);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("periodCode", periodCode);
        AccountingPeriod period = accountingPeriodMapper.findByPeriodCode(currentTenantId(), periodCode).orElse(null);
        if (period == null) {
            map.put("exists", false);
            map.put("statusText", "期间不存在");
            return map;
        }
        map.put("exists", true);
        map.put("periodId", period.getId());
        map.put("status", period.getStatus());
        map.put("statusText", period.getStatus() != null && period.getStatus() == 1 ? "未结账" : "已结账");
        map.put("startDate", period.getStartDate());
        map.put("endDate", period.getEndDate());
        map.put("closedBy", period.getClosedBy());
        map.put("closedTime", period.getClosedTime());
        monthClosingLogMapper.findLatestByPeriodCode(currentTenantId(), periodCode)
                .ifPresent(latest -> map.put("latestLog", latest));
        return map;
    }

    @Override
    public IPage<MonthClosingLog> logPage(String periodCode, Page<MonthClosingLog> page) {
        LambdaQueryWrapper<MonthClosingLog> wrapper = new LambdaQueryWrapper<MonthClosingLog>()
                .eq(MonthClosingLog::getTenantId, currentTenantId())
                .eq(periodCode != null && !periodCode.isBlank(), MonthClosingLog::getPeriodCode, periodCode)
                .orderByDesc(MonthClosingLog::getId);
        return monthClosingLogMapper.selectPage(page, wrapper);
    }

    /**
     * 月结检查项（P0/P1/P2）
     */
    private List<MonthClosingResultDTO.CheckItem> runChecks(int year, int month) {
        List<MonthClosingResultDTO.CheckItem> checks = new ArrayList<>();
        List<Voucher> vouchers = voucherMapper.findByFiscalYearAndFiscalPeriod(year, month);

        // 检查1：借贷平衡校验（P1，期间内所有凭证借=贷，存在未平衡凭证阻止关账）
        List<Voucher> unbalanced = vouchers.stream()
                .filter(v -> !isBalanced(v))
                .collect(Collectors.toList());
        MonthClosingResultDTO.CheckItem balance = new MonthClosingResultDTO.CheckItem();
        balance.setCheckCode("BALANCE_CHECK");
        balance.setCheckName("借贷平衡校验");
        balance.setPassed(unbalanced.isEmpty());
        if (unbalanced.isEmpty()) {
            balance.setDetail("期间内所有凭证借贷平衡");
        } else {
            String voucherList = unbalanced.stream()
                    .limit(DETAIL_LIMIT)
                    .map(v -> v.getVoucherNo() + "(借:" + nvl(v.getTotalDebit()) + "/贷:" + nvl(v.getTotalCredit()) + ")")
                    .collect(Collectors.joining(", "));
            balance.setDetail(String.format("存在 %d 张借贷不平的凭证: %s%s",
                    unbalanced.size(), voucherList, unbalanced.size() > DETAIL_LIMIT ? " ..." : ""));
        }
        checks.add(balance);

        // 检查2：未过账凭证（草稿/已审核）不得存在
        List<Voucher> unposted = vouchers.stream()
                .filter(v -> "draft".equals(v.getStatus()) || "audited".equals(v.getStatus()))
                .collect(Collectors.toList());
        MonthClosingResultDTO.CheckItem vCheck = new MonthClosingResultDTO.CheckItem();
        vCheck.setCheckCode("UNPOSTED_VOUCHER");
        vCheck.setCheckName("未过账凭证检查");
        vCheck.setPassed(unposted.isEmpty());
        if (unposted.isEmpty()) {
            vCheck.setDetail("该期间无未过账凭证");
        } else {
            String voucherList = unposted.stream()
                    .limit(DETAIL_LIMIT)
                    .map(v -> v.getVoucherNo() + "(" + v.getStatus() + ")")
                    .collect(Collectors.joining(", "));
            vCheck.setDetail(String.format("存在 %d 张未过账凭证: %s%s",
                    unposted.size(), voucherList, unposted.size() > DETAIL_LIMIT ? " ..." : ""));
        }
        checks.add(vCheck);

        // 检查3（提示项）：已过账/已冲销凭证统计，不阻断
        long posted = vouchers.stream().filter(v -> "posted".equals(v.getStatus())).count();
        long reversed = vouchers.stream().filter(v -> "reversed".equals(v.getStatus())).count();
        MonthClosingResultDTO.CheckItem summary = new MonthClosingResultDTO.CheckItem();
        summary.setCheckCode("VOUCHER_SUMMARY");
        summary.setCheckName("凭证汇总信息");
        summary.setPassed(true);
        summary.setDetail(String.format("期间共 %d 张凭证，已过账 %d 张，已冲销 %d 张",
                vouchers.size(), posted, reversed));
        checks.add(summary);

        // 检查4（提示项）：损益结转汇总（P1，展示本期可结转损益科目数）
        List<AccountSubject> profitLossSubjects = accountSubjectMapper.findBySubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        BigDecimal carryOverAmount = sumProfitLossNet(year, month, profitLossSubjects);
        MonthClosingResultDTO.CheckItem carryCheck = new MonthClosingResultDTO.CheckItem();
        carryCheck.setCheckCode("PROFIT_LOSS_CARRY");
        carryCheck.setCheckName("期末损益结转");
        carryCheck.setPassed(true);
        if (carryOverAmount.compareTo(BigDecimal.ZERO) == 0) {
            carryCheck.setDetail("本期无可结转损益，无需生成结转凭证");
        } else {
            carryCheck.setDetail(String.format("本期损益类科目净发生额合计 %s，月结时将结转至本年利润(4103)", carryOverAmount));
        }
        checks.add(carryCheck);

        return checks;
    }

    /**
     * 期末结转损益：将期间内损益类科目发生额结转至本年利润 4103，生成 KJPZ- 结转凭证。
     * 借方净额（费用/成本）→ 借 4103 / 贷 该科目；贷方净额（收入）→ 借 该科目 / 贷 4103。
     *
     * @return 结转凭证号；本期无可结转损益时返回 null
     */
    private String carryOverProfitLoss(int year, int month, AccountingPeriod period, String periodCode) {
        List<AccountSubject> profitLossSubjects = accountSubjectMapper.findBySubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        if (profitLossSubjects.isEmpty()) {
            return null;
        }

        Map<Long, LedgerEntry> ledgerMap = ledgerEntryMapper.findByFiscalYear(year).stream()
                .filter(e -> e.getFiscalPeriod() != null && e.getFiscalPeriod() == month)
                .collect(Collectors.toMap(LedgerEntry::getSubjectId, e -> e, (a, b) -> a));

        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        for (AccountSubject subj : profitLossSubjects) {
            LedgerEntry entry = ledgerMap.get(subj.getId());
            if (entry == null) {
                continue;
            }
            BigDecimal periodDebit = nvl(entry.getPeriodDebit());
            BigDecimal periodCredit = nvl(entry.getPeriodCredit());
            BigDecimal net = periodDebit.subtract(periodCredit);
            if (net.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BusinessAccountingRequest.AccountingRequestItem debit = new BusinessAccountingRequest.AccountingRequestItem();
            BusinessAccountingRequest.AccountingRequestItem credit = new BusinessAccountingRequest.AccountingRequestItem();
            debit.setSummary("结转损益-" + subj.getSubjectName());
            credit.setSummary("结转损益-" + subj.getSubjectName());

            if (net.compareTo(BigDecimal.ZERO) > 0) {
                // 费用/成本（借方净额）：借 本年利润 4103 / 贷 该损益科目
                debit.setSubjectCode(SUBJECT_YEAR_PROFIT);
                debit.setDebitAmount(net);
                debit.setCreditAmount(BigDecimal.ZERO);
                credit.setSubjectCode(subj.getSubjectCode());
                credit.setDebitAmount(BigDecimal.ZERO);
                credit.setCreditAmount(net);
            } else {
                // 收入（贷方净额）：借 该损益科目 / 贷 本年利润 4103
                BigDecimal absNet = net.abs();
                debit.setSubjectCode(subj.getSubjectCode());
                debit.setDebitAmount(absNet);
                debit.setCreditAmount(BigDecimal.ZERO);
                credit.setSubjectCode(SUBJECT_YEAR_PROFIT);
                credit.setDebitAmount(BigDecimal.ZERO);
                credit.setCreditAmount(absNet);
            }
            items.add(debit);
            items.add(credit);
        }

        if (items.isEmpty()) {
            return null;
        }

        BusinessAccountingRequest request = new BusinessAccountingRequest();
        request.setSourceType("MONTH_CLOSING");
        request.setSourceNo(periodCode);
        request.setSummary("期末结转损益 - " + periodCode);
        request.setVoucherDate(period.getEndDate() != null ? period.getEndDate() : LocalDate.of(year, month, 1));
        request.setItems(items);

        try {
            VoucherDTO voucher = businessAccountingService.createVoucherFromBusiness(request);
            log.info("期末结转损益生成凭证: period={}, voucherNo={}, items={}", periodCode, voucher.getVoucherNo(), items.size());
            return voucher.getVoucherNo();
        } catch (BusinessException e) {
            // 结转失败（科目缺失等）阻止关账，保证损益已结转后才关闭期间
            log.error("期末结转损益生成凭证失败: period={}, error={}", periodCode, e.getMessage());
            throw BusinessException.badRequest("期末结转损益失败，期间未关闭: " + e.getMessage());
        }
    }

    /**
     * 计算期间内损益类科目净发生额合计（借方-贷方绝对值），用于提示
     */
    private BigDecimal sumProfitLossNet(int year, int month, List<AccountSubject> subjects) {
        Map<Long, LedgerEntry> ledgerMap = ledgerEntryMapper.findByFiscalYear(year).stream()
                .filter(e -> e.getFiscalPeriod() != null && e.getFiscalPeriod() == month
                        && subjects.stream().anyMatch(s -> s.getId().equals(e.getSubjectId())))
                .collect(Collectors.toMap(LedgerEntry::getSubjectId, e -> e, (a, b) -> a));
        BigDecimal total = BigDecimal.ZERO;
        for (AccountSubject subj : subjects) {
            LedgerEntry entry = ledgerMap.get(subj.getId());
            if (entry == null) {
                continue;
            }
            BigDecimal net = nvl(entry.getPeriodDebit()).subtract(nvl(entry.getPeriodCredit()));
            if (net.compareTo(BigDecimal.ZERO) != 0) {
                total = total.add(net.abs());
            }
        }
        return total;
    }

    private boolean isBalanced(Voucher v) {
        if (v.getTotalDebit() == null || v.getTotalCredit() == null) {
            return false;
        }
        return v.getTotalDebit().compareTo(v.getTotalCredit()) == 0;
    }

    private void writeLog(String periodCode, String action, String operatorId, String operatorName, String checkResult) {
        MonthClosingLog logEntity = new MonthClosingLog();
        logEntity.setTenantId(currentTenantId());
        logEntity.setPeriodCode(periodCode);
        logEntity.setAction(action);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setCheckResult(checkResult);
        logEntity.setCreateTime(LocalDateTime.now());
        monthClosingLogMapper.insert(logEntity);
    }

    private String buildCheckResultJson(List<MonthClosingResultDTO.CheckItem> checks, boolean passed, String carryOverVoucherNo) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("passed", passed);
            map.put("checks", checks);
            if (carryOverVoucherNo != null) {
                map.put("carryOverVoucherNo", carryOverVoucherNo);
            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("月结检查结果序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private AccountingPeriod getPeriodOrThrow(String periodCode) {
        return accountingPeriodMapper.findByPeriodCode(currentTenantId(), periodCode)
                .orElseThrow(() -> BusinessException.notFound("会计期间不存在: " + periodCode));
    }

    /**
     * 解析期间编码 yyyy-MM，返回 [year, month]
     */
    private int[] parsePeriodCode(String periodCode) {
        if (periodCode == null) {
            throw BusinessException.badRequest("期间编码不能为空");
        }
        Matcher matcher = PERIOD_CODE_PATTERN.matcher(periodCode.trim());
        if (!matcher.matches()) {
            throw BusinessException.badRequest("期间编码格式错误，应为 yyyy-MM，如 2026-07: " + periodCode);
        }
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
