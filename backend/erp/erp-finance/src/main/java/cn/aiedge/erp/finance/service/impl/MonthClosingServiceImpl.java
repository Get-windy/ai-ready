package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.MonthClosingResultDTO;
import cn.aiedge.erp.finance.mapper.AccountingPeriodMapper;
import cn.aiedge.erp.finance.mapper.MonthClosingLogMapper;
import cn.aiedge.erp.finance.mapper.VoucherMapper;
import cn.aiedge.erp.finance.model.entity.AccountingPeriod;
import cn.aiedge.erp.finance.model.entity.MonthClosingLog;
import cn.aiedge.erp.finance.model.entity.Voucher;
import cn.aiedge.erp.finance.service.MonthClosingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 月结校验（对标金蝶/用友总账月结）：
 * 1. 期间必须存在且处于开启状态
 * 2. 该期间不得存在未过账凭证（finance_voucher.status 为 draft/audited；
 *    posted=已过账通过，reversed=已冲销不阻断）
 * 全部通过则关闭期间（status=0）并写入月结日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthClosingServiceImpl implements MonthClosingService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Pattern PERIOD_CODE_PATTERN = Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])$");
    /**
     * 未过账凭证明细快照上限
     */
    private static final int DETAIL_LIMIT = 20;

    private final AccountingPeriodMapper accountingPeriodMapper;
    private final MonthClosingLogMapper monthClosingLogMapper;
    private final VoucherMapper voucherMapper;
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

        // 关闭期间
        LocalDateTime now = LocalDateTime.now();
        period.setStatus(0);
        period.setClosedBy(operatorName != null ? operatorName : operatorId);
        period.setClosedTime(now);
        accountingPeriodMapper.updateById(period);

        // 写月结日志（含检查结果快照）
        writeLog(periodCode, "close", operatorId, operatorName, buildCheckResultJson(checks, true));

        result.setSuccess(true);
        result.setMessage("月结成功，期间已关闭");
        result.setClosedBy(period.getClosedBy());
        result.setClosedTime(now);
        log.info("月结成功: period={}, operator={}", periodCode, operatorId);
        return result;
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
        AccountingPeriod period = accountingPeriodMapper.findByPeriodCode(DEFAULT_TENANT_ID, periodCode).orElse(null);
        if (period == null) {
            map.put("exists", false);
            map.put("statusText", "期间不存在");
            return map;
        }
        map.put("exists", true);
        map.put("periodId", period.getId());
        map.put("status", period.getStatus());
        map.put("statusText", period.getStatus() != null && period.getStatus() == 1 ? "开启" : "关闭（已月结）");
        map.put("startDate", period.getStartDate());
        map.put("endDate", period.getEndDate());
        map.put("closedBy", period.getClosedBy());
        map.put("closedTime", period.getClosedTime());
        monthClosingLogMapper.findLatestByPeriodCode(DEFAULT_TENANT_ID, periodCode)
                .ifPresent(latest -> map.put("latestLog", latest));
        return map;
    }

    @Override
    public IPage<MonthClosingLog> logPage(String periodCode, Page<MonthClosingLog> page) {
        LambdaQueryWrapper<MonthClosingLog> wrapper = new LambdaQueryWrapper<MonthClosingLog>()
                .eq(MonthClosingLog::getTenantId, DEFAULT_TENANT_ID)
                .eq(periodCode != null && !periodCode.isBlank(), MonthClosingLog::getPeriodCode, periodCode)
                .orderByDesc(MonthClosingLog::getId);
        return monthClosingLogMapper.selectPage(page, wrapper);
    }

    /**
     * 月结检查项
     */
    private List<MonthClosingResultDTO.CheckItem> runChecks(int year, int month) {
        List<MonthClosingResultDTO.CheckItem> checks = new ArrayList<>();

        // 检查1：未过账凭证（草稿/已审核）不得存在
        LambdaQueryWrapper<Voucher> unpostedWrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getFiscalYear, year)
                .eq(Voucher::getFiscalPeriod, month)
                .in(Voucher::getStatus, "draft", "audited");
        Long unpostedCount = voucherMapper.selectCount(unpostedWrapper);

        MonthClosingResultDTO.CheckItem unposted = new MonthClosingResultDTO.CheckItem();
        unposted.setCheckCode("UNPOSTED_VOUCHER");
        unposted.setCheckName("未过账凭证检查");
        unposted.setPassed(unpostedCount == 0);
        if (unpostedCount == 0) {
            unposted.setDetail("该期间无未过账凭证");
        } else {
            List<Voucher> samples = voucherMapper.selectList(unpostedWrapper
                    .select(Voucher::getVoucherNo, Voucher::getStatus)
                    .last("LIMIT " + DETAIL_LIMIT));
            String voucherList = samples.stream()
                    .map(v -> v.getVoucherNo() + "(" + v.getStatus() + ")")
                    .collect(Collectors.joining(", "));
            unposted.setDetail(String.format("存在 %d 张未过账凭证: %s%s",
                    unpostedCount, voucherList, unpostedCount > DETAIL_LIMIT ? " ..." : ""));
        }
        checks.add(unposted);

        // 检查2（提示项）：已过账/已冲销凭证统计，不阻断
        Long postedCount = voucherMapper.selectCount(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getFiscalYear, year)
                .eq(Voucher::getFiscalPeriod, month)
                .eq(Voucher::getStatus, "posted"));
        Long reversedCount = voucherMapper.selectCount(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getFiscalYear, year)
                .eq(Voucher::getFiscalPeriod, month)
                .eq(Voucher::getStatus, "reversed"));
        MonthClosingResultDTO.CheckItem summary = new MonthClosingResultDTO.CheckItem();
        summary.setCheckCode("VOUCHER_SUMMARY");
        summary.setCheckName("凭证汇总信息");
        summary.setPassed(true);
        summary.setDetail(String.format("已过账 %d 张，已冲销 %d 张", postedCount, reversedCount));
        checks.add(summary);

        return checks;
    }

    private void writeLog(String periodCode, String action, String operatorId, String operatorName, String checkResult) {
        MonthClosingLog logEntity = new MonthClosingLog();
        logEntity.setTenantId(DEFAULT_TENANT_ID);
        logEntity.setPeriodCode(periodCode);
        logEntity.setAction(action);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperatorName(operatorName);
        logEntity.setCheckResult(checkResult);
        logEntity.setCreateTime(LocalDateTime.now());
        monthClosingLogMapper.insert(logEntity);
    }

    private String buildCheckResultJson(List<MonthClosingResultDTO.CheckItem> checks, boolean passed) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("passed", passed);
            map.put("checks", checks);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("月结检查结果序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private AccountingPeriod getPeriodOrThrow(String periodCode) {
        return accountingPeriodMapper.findByPeriodCode(DEFAULT_TENANT_ID, periodCode)
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
}
