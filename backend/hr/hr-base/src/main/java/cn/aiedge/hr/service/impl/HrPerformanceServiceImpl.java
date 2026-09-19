package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.performance.HrPerformance;
import cn.aiedge.hr.mapper.HrPerformanceMapper;
import cn.aiedge.hr.service.HrPerformanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 绩效考核服务实现
 *
 * <p>等级值域 S/A/B/C/D。`performance_coefficient`（绩效系数）由考核人填写，为薪资侧的
 * **绩效工资联动**提供输入；未填时按 1.0 处理（不做调整）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPerformanceServiceImpl extends ServiceImpl<HrPerformanceMapper, HrPerformance>
        implements HrPerformanceService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_REVIEWED = 1;
    private static final int STATUS_CONFIRMED = 2;

    private static final List<String> LEVELS = Arrays.asList("S", "A", "B", "C", "D");

    private final HrLookupHelper lookupHelper;

    @Override
    public Page<HrPerformance> pageReviews(Page<HrPerformance> page, Long tenantId,
                                           Long employeeId, String reviewPeriod,
                                           String reviewType, String level, Integer status, Long deptId) {
        LambdaQueryWrapper<HrPerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrPerformance::getTenantId, tenantId)
                .eq(employeeId != null, HrPerformance::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(reviewPeriod), HrPerformance::getReviewPeriod, reviewPeriod)
                .eq(StringUtils.hasText(reviewType), HrPerformance::getReviewType, reviewType)
                .eq(StringUtils.hasText(level), HrPerformance::getLevel, level)
                .eq(status != null, HrPerformance::getStatus, status);
        applyDeptScope(wrapper, deptId);
        wrapper.orderByDesc(HrPerformance::getReviewPeriod).orderByDesc(HrPerformance::getId);
        Page<HrPerformance> result = page(page, wrapper);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrPerformance> listForExport(String reviewPeriod, String reviewType,
                                             String level, Integer status, Long deptId) {
        LambdaQueryWrapper<HrPerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(reviewPeriod), HrPerformance::getReviewPeriod, reviewPeriod)
                .eq(StringUtils.hasText(reviewType), HrPerformance::getReviewType, reviewType)
                .eq(StringUtils.hasText(level), HrPerformance::getLevel, level)
                .eq(status != null, HrPerformance::getStatus, status);
        applyDeptScope(wrapper, deptId);
        List<HrPerformance> rows = list(wrapper.orderByDesc(HrPerformance::getReviewPeriod));
        enrich(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitReview(HrPerformance performance) {
        validate(performance);
        performance.setStatus(STATUS_REVIEWED);
        performance.setReviewTime(LocalDateTime.now());
        if (performance.getReviewerId() == null) {
            performance.setReviewerId(SecurityUtils.getCurrentUserId());
        }
        if (!StringUtils.hasText(performance.getReviewerName())) {
            performance.setReviewerName(SecurityUtils.getCurrentUsername());
        }
        if (performance.getTenantId() == null) {
            performance.setTenantId(SecurityUtils.getCurrentTenantId());
        }
        performance.setCreateTime(LocalDateTime.now());
        performance.setUpdateTime(LocalDateTime.now());
        save(performance);
        log.info("提交绩效考核: employeeId={}, period={}", performance.getEmployeeId(), performance.getReviewPeriod());
        return performance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReview(HrPerformance performance) {
        if (performance.getId() == null) {
            throw new BusinessException("考核记录ID不能为空");
        }
        HrPerformance exists = getById(performance.getId());
        if (exists == null) {
            throw new BusinessException("考核记录不存在");
        }
        if (exists.getStatus() != null && exists.getStatus() == STATUS_CONFIRMED) {
            throw new BusinessException("已确认的考核不能修改");
        }
        performance.setEmployeeId(null);
        performance.setStatus(null);
        performance.setReviewerId(null);
        performance.setUpdateTime(LocalDateTime.now());
        updateById(performance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReview(Long id) {
        HrPerformance exists = require(id);
        if (exists.getStatus() != null && exists.getStatus() == STATUS_CONFIRMED) {
            throw new BusinessException("该考核已确认，不能重复确认");
        }
        LambdaUpdateWrapper<HrPerformance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrPerformance::getId, id)
                .set(HrPerformance::getStatus, STATUS_CONFIRMED)
                .set(HrPerformance::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        log.info("确认绩效考核: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id) {
        HrPerformance exists = require(id);
        if (exists.getStatus() != null && exists.getStatus() == STATUS_CONFIRMED) {
            throw new BusinessException("已确认的考核不能删除，请先驳回或作废");
        }
        removeById(id);
    }

    @Override
    public Map<String, Object> statistics(String reviewPeriod, Long deptId) {
        LambdaQueryWrapper<HrPerformance> wrapper = new LambdaQueryWrapper<HrPerformance>()
                .eq(StringUtils.hasText(reviewPeriod), HrPerformance::getReviewPeriod, reviewPeriod);
        applyDeptScope(wrapper, deptId);
        List<HrPerformance> rows = list(wrapper);

        Map<String, Object> levelDistribution = new LinkedHashMap<>();
        for (String level : LEVELS) {
            levelDistribution.put(level, rows.stream()
                    .filter(r -> level.equals(r.getLevel())).count());
        }
        BigDecimal avgScore = rows.stream().map(HrPerformance::getScore).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long scored = rows.stream().filter(r -> r.getScore() != null).count();

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("reviewPeriod", reviewPeriod);
        stat.put("total", rows.size());
        stat.put("pendingCount", rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_PENDING).count());
        stat.put("reviewedCount", rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_REVIEWED).count());
        stat.put("confirmedCount", rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_CONFIRMED).count());
        stat.put("averageScore", scored > 0
                ? avgScore.divide(BigDecimal.valueOf(scored), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        stat.put("levelDistribution", levelDistribution);
        return stat;
    }

    // ── 内部 ──────────────────────────────────────────────

    private void validate(HrPerformance performance) {
        if (performance.getEmployeeId() == null) {
            throw new BusinessException("请选择被考核员工");
        }
        if (!StringUtils.hasText(performance.getReviewPeriod())) {
            throw new BusinessException("请填写考核周期");
        }
        if (performance.getScore() != null
                && (performance.getScore().compareTo(BigDecimal.ZERO) < 0
                || performance.getScore().compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException("考核评分必须在 0 ~ 100 之间");
        }
        if (StringUtils.hasText(performance.getLevel()) && !LEVELS.contains(performance.getLevel())) {
            throw new BusinessException("非法的考核等级：" + performance.getLevel());
        }
        if (performance.getPerformanceCoefficient() != null
                && performance.getPerformanceCoefficient().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("绩效系数不能为负数");
        }
    }

    private HrPerformance require(Long id) {
        HrPerformance performance = id == null ? null : getById(id);
        if (performance == null) {
            throw new BusinessException("考核记录不存在");
        }
        return performance;
    }

    private void applyDeptScope(LambdaQueryWrapper<HrPerformance> wrapper, Long deptId) {
        if (deptId == null) {
            return;
        }
        List<Long> ids = lookupHelper.employeeIdsOfDept(deptId);
        if (ids.isEmpty()) {
            wrapper.eq(HrPerformance::getId, -1L);
        } else {
            wrapper.in(HrPerformance::getEmployeeId, ids);
        }
    }

    private void enrich(List<HrPerformance> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrPerformance::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> deptNameByEmp = lookupHelper.deptNameByEmployeeId(empMap.keySet());
        for (HrPerformance p : rows) {
            HrEmployee emp = empMap.get(p.getEmployeeId());
            if (emp != null) {
                p.setEmployeeName(emp.getEmployeeName());
                p.setEmployeeNo(emp.getEmployeeNo());
            }
            p.setDeptName(deptNameByEmp.get(p.getEmployeeId()));
        }
    }
}
