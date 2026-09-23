package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.attendance.HrAttendanceRule;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrAttendanceMapper;
import cn.aiedge.hr.mapper.HrAttendanceRuleMapper;
import cn.aiedge.hr.service.HrAttendanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 考勤记录服务实现
 *
 * <p>与旧实现的关键差别：</p>
 * <ol>
 *   <li><b>打卡即算</b>：写入时按「考勤规则」计算迟到分钟、早退分钟与工时，不再恒为 NORMAL / 空值；</li>
 *   <li><b>考勤规则可配</b>（`hr_attendance_rule`）：上下班时间、迟到/早退宽限、标准日工时；</li>
 *   <li><b>补卡与更正</b>：支持手工建档 / 修改 / 删除（考勤机异常与忘打卡的更正入口）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrAttendanceServiceImpl extends ServiceImpl<HrAttendanceMapper, HrAttendance>
        implements HrAttendanceService {

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_LATE = "LATE";
    public static final String STATUS_EARLY = "EARLY";
    public static final String STATUS_ABSENT = "ABSENT";
    public static final String STATUS_LEAVE = "LEAVE";

    private static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(18, 0);
    private static final BigDecimal DEFAULT_STANDARD_HOURS = new BigDecimal("8");

    private final HrAttendanceRuleMapper ruleMapper;
    private final HrLookupHelper lookupHelper;

    // ── 考勤规则 ──────────────────────────────────────────

    @Override
    public HrAttendanceRule getRule() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        HrAttendanceRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<HrAttendanceRule>()
                .eq(HrAttendanceRule::getTenantId, tenantId)
                .orderByAsc(HrAttendanceRule::getId)
                .last("limit 1"));
        if (rule == null) {
            rule = defaultRule(tenantId);
        }
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrAttendanceRule saveRule(HrAttendanceRule rule) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        HrAttendanceRule exists = ruleMapper.selectOne(new LambdaQueryWrapper<HrAttendanceRule>()
                .eq(HrAttendanceRule::getTenantId, tenantId)
                .orderByAsc(HrAttendanceRule::getId)
                .last("limit 1"));
        HrAttendanceRule target = exists == null ? defaultRule(tenantId) : exists;
        if (rule.getWorkStartTime() != null) {
            target.setWorkStartTime(rule.getWorkStartTime());
        }
        if (rule.getWorkEndTime() != null) {
            target.setWorkEndTime(rule.getWorkEndTime());
        }
        if (rule.getLateGraceMinutes() != null) {
            target.setLateGraceMinutes(rule.getLateGraceMinutes());
        }
        if (rule.getEarlyGraceMinutes() != null) {
            target.setEarlyGraceMinutes(rule.getEarlyGraceMinutes());
        }
        if (rule.getStandardWorkHours() != null) {
            target.setStandardWorkHours(rule.getStandardWorkHours());
        }
        if (rule.getAutoAbsent() != null) {
            target.setAutoAbsent(rule.getAutoAbsent());
        }
        if (rule.getRemark() != null) {
            target.setRemark(rule.getRemark());
        }
        target.setUpdateTime(LocalDateTime.now());
        if (target.getId() == null) {
            ruleMapper.insert(target);
        } else {
            ruleMapper.updateById(target);
        }
        log.info("保存考勤规则: tenantId={}, start={}, end={}", tenantId,
                target.getWorkStartTime(), target.getWorkEndTime());
        return target;
    }

    private HrAttendanceRule defaultRule(Long tenantId) {
        return new HrAttendanceRule()
                .setTenantId(tenantId)
                .setWorkStartTime(DEFAULT_START)
                .setWorkEndTime(DEFAULT_END)
                .setLateGraceMinutes(0)
                .setEarlyGraceMinutes(0)
                .setStandardWorkHours(DEFAULT_STANDARD_HOURS)
                .setAutoAbsent(0);
    }

    // ── 查询 ──────────────────────────────────────────────

    @Override
    public Page<HrAttendance> pageAttendances(Page<HrAttendance> page, Long tenantId,
                                              Long employeeId, String month,
                                              String status, Long deptId,
                                              LocalDate dateStart, LocalDate dateEnd) {
        LambdaQueryWrapper<HrAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrAttendance::getTenantId, tenantId)
                .eq(employeeId != null, HrAttendance::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(status), HrAttendance::getStatus, status);
        applyMonthRange(wrapper, month);
        wrapper.ge(dateStart != null, HrAttendance::getAttendanceDate, dateStart)
                .le(dateEnd != null, HrAttendance::getAttendanceDate, dateEnd);
        applyDeptScope(wrapper, deptId);
        wrapper.orderByDesc(HrAttendance::getAttendanceDate).orderByDesc(HrAttendance::getId);
        Page<HrAttendance> result = page(page, wrapper);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrAttendance> getByEmployeeAndMonth(Long employeeId, String month) {
        if (employeeId == null) {
            return List.of();
        }
        LambdaQueryWrapper<HrAttendance> wrapper = new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getEmployeeId, employeeId);
        applyMonthRange(wrapper, month);
        return list(wrapper.orderByAsc(HrAttendance::getAttendanceDate));
    }

    /** month 用区间匹配，避免旧实现 `like('2026-09')` 命中 `2026-0901` 这类脏数据 */
    private void applyMonthRange(LambdaQueryWrapper<HrAttendance> wrapper, String month) {
        if (!StringUtils.hasText(month)) {
            return;
        }
        try {
            YearMonth ym = YearMonth.parse(month.trim());
            wrapper.ge(HrAttendance::getAttendanceDate, ym.atDay(1))
                    .le(HrAttendance::getAttendanceDate, ym.atEndOfMonth());
        } catch (DateTimeParseException e) {
            throw new BusinessException("月份格式错误，应为 yyyy-MM：" + month);
        }
    }

    /** 按部门筛选：先取该部门员工 id 再 in；部门无员工时用不可能命中的条件短路 */
    private void applyDeptScope(LambdaQueryWrapper<HrAttendance> wrapper, Long deptId) {
        if (deptId == null) {
            return;
        }
        List<Long> ids = lookupHelper.employeeIdsOfDept(deptId);
        if (ids.isEmpty()) {
            wrapper.eq(HrAttendance::getId, -1L);
        } else {
            wrapper.in(HrAttendance::getEmployeeId, ids);
        }
    }

    // ── 打卡 ──────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clockIn(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException("员工ID不能为空");
        }
        LocalDate today = LocalDate.now();
        HrAttendance exists = getOne(new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getEmployeeId, employeeId)
                .eq(HrAttendance::getAttendanceDate, today)
                .last("limit 1"));
        if (exists != null && exists.getClockInTime() != null) {
            throw new BusinessException("今日已签到，请勿重复签到");
        }
        HrAttendanceRule rule = getRule();
        HrAttendance attendance = exists != null ? exists : new HrAttendance();
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setClockInTime(LocalTime.now().withNano(0));
        if (attendance.getTenantId() == null) {
            attendance.setTenantId(SecurityUtils.getCurrentTenantId());
        }
        recompute(attendance, rule);
        attendance.setUpdateTime(LocalDateTime.now());
        if (attendance.getId() == null) {
            attendance.setCreateTime(LocalDateTime.now());
            save(attendance);
        } else {
            updateById(attendance);
        }
        log.info("上班打卡: employeeId={}, time={}, status={}", employeeId, attendance.getClockInTime(), attendance.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clockOut(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException("员工ID不能为空");
        }
        HrAttendance attendance = getOne(new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getEmployeeId, employeeId)
                .eq(HrAttendance::getAttendanceDate, LocalDate.now())
                .last("limit 1"));
        if (attendance == null || attendance.getClockInTime() == null) {
            throw new BusinessException("今日尚未签到，无法签退");
        }
        attendance.setClockOutTime(LocalTime.now().withNano(0));
        recompute(attendance, getRule());
        attendance.setUpdateTime(LocalDateTime.now());
        updateById(attendance);
        log.info("下班打卡: employeeId={}, time={}, status={}", employeeId, attendance.getClockOutTime(), attendance.getStatus());
    }

    // ── 建档 / 更正 / 删除 ─────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAttendance(HrAttendance attendance) {
        if (attendance.getEmployeeId() == null) {
            throw new BusinessException("请选择员工");
        }
        if (attendance.getAttendanceDate() == null) {
            throw new BusinessException("考勤日期不能为空");
        }
        Long dup = baseMapper.selectCount(new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getEmployeeId, attendance.getEmployeeId())
                .eq(HrAttendance::getAttendanceDate, attendance.getAttendanceDate()));
        if (dup != null && dup > 0) {
            throw new BusinessException("该员工当日已有考勤记录，请直接修改");
        }
        attendance.setTenantId(SecurityUtils.getCurrentTenantId());
        recompute(attendance, getRule());
        attendance.setCreateTime(LocalDateTime.now());
        attendance.setUpdateTime(LocalDateTime.now());
        save(attendance);
        return attendance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAttendance(HrAttendance attendance) {
        if (attendance.getId() == null) {
            throw new BusinessException("考勤ID不能为空");
        }
        HrAttendance exists = getById(attendance.getId());
        if (exists == null) {
            throw new BusinessException("考勤记录不存在");
        }
        attendance.setEmployeeId(null);
        attendance.setAttendanceDate(null);
        attendance.setTenantId(null);
        recompute(attendance, getRule());
        attendance.setUpdateTime(LocalDateTime.now());
        updateById(attendance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttendance(Long id) {
        if (id == null || getById(id) == null) {
            throw new BusinessException("考勤记录不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的考勤记录");
        }
        removeByIds(ids);
        log.info("批量删除考勤记录: count={}", ids.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(Long id) {
        HrAttendance attendance = id == null ? null : getById(id);
        if (attendance == null) {
            throw new BusinessException("考勤记录不存在");
        }
        recompute(attendance, getRule());
        attendance.setUpdateTime(LocalDateTime.now());
        updateById(attendance);
    }

    // ── 统计 ──────────────────────────────────────────────

    @Override
    public Map<String, Object> statistics(String month, Long deptId, Long employeeId) {
        LambdaQueryWrapper<HrAttendance> wrapper = new LambdaQueryWrapper<HrAttendance>()
                .eq(employeeId != null, HrAttendance::getEmployeeId, employeeId);
        applyMonthRange(wrapper, month);
        applyDeptScope(wrapper, deptId);
        List<HrAttendance> rows = list(wrapper);
        long normal = rows.stream().filter(a -> STATUS_NORMAL.equals(a.getStatus())).count();
        long late = rows.stream().filter(a -> STATUS_LATE.equals(a.getStatus())).count();
        long early = rows.stream().filter(a -> STATUS_EARLY.equals(a.getStatus())).count();
        long absent = rows.stream().filter(a -> STATUS_ABSENT.equals(a.getStatus())).count();
        long leave = rows.stream().filter(a -> STATUS_LEAVE.equals(a.getStatus())).count();
        BigDecimal totalHours = rows.stream()
                .map(HrAttendance::getWorkHours).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overtimeHours = rows.stream()
                .map(HrAttendance::getWorkHours).filter(Objects::nonNull)
                .filter(h -> h.compareTo(DEFAULT_STANDARD_HOURS) > 0)
                .map(h -> h.subtract(DEFAULT_STANDARD_HOURS))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("recordCount", rows.size());
        stat.put("normalDays", normal);
        stat.put("lateDays", late);
        stat.put("earlyDays", early);
        stat.put("absentDays", absent);
        stat.put("leaveDays", leave);
        stat.put("totalWorkHours", totalHours.setScale(2, RoundingMode.HALF_UP));
        stat.put("overtimeHours", overtimeHours.setScale(2, RoundingMode.HALF_UP));
        return stat;
    }

    // ── 内部 ──────────────────────────────────────────────

    /**
     * 按考勤规则重算状态 / 迟到分钟 / 早退分钟 / 工时。
     * 已标记为「休假」的记录不参与迟到早退判定（由请假批准写入）。
     */
    private void recompute(HrAttendance attendance, HrAttendanceRule rule) {
        if (STATUS_LEAVE.equals(attendance.getStatus())) {
            return;
        }
        LocalTime start = rule.getWorkStartTime() == null ? DEFAULT_START : rule.getWorkStartTime();
        LocalTime end = rule.getWorkEndTime() == null ? DEFAULT_END : rule.getWorkEndTime();
        int lateGrace = rule.getLateGraceMinutes() == null ? 0 : rule.getLateGraceMinutes();
        int earlyGrace = rule.getEarlyGraceMinutes() == null ? 0 : rule.getEarlyGraceMinutes();

        LocalTime in = attendance.getClockInTime();
        LocalTime out = attendance.getClockOutTime();

        int late = 0;
        int early = 0;
        if (in != null) {
            LocalTime lateThreshold = start.plusMinutes(lateGrace);
            if (in.isAfter(lateThreshold)) {
                late = (int) Duration.between(start, in).toMinutes();
            }
        }
        if (out != null) {
            LocalTime earlyThreshold = end.minusMinutes(earlyGrace);
            if (out.isBefore(earlyThreshold)) {
                early = (int) Duration.between(out, end).toMinutes();
            }
        }
        attendance.setLateMinutes(late);
        attendance.setEarlyMinutes(early);

        if (in != null && out != null && out.isAfter(in)) {
            BigDecimal hours = BigDecimal.valueOf(Duration.between(in, out).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            attendance.setWorkHours(hours);
        } else {
            attendance.setWorkHours(BigDecimal.ZERO);
        }

        if (late > 0) {
            // 迟到优先于早退（同一天两者都有时，迟到是更严重的判定，前端仍可看到早退分钟数）
            attendance.setStatus(STATUS_LATE);
        } else if (early > 0) {
            attendance.setStatus(STATUS_EARLY);
        } else if (in == null && out == null) {
            attendance.setStatus(STATUS_ABSENT);
        } else {
            attendance.setStatus(STATUS_NORMAL);
        }
    }

    private void enrich(List<HrAttendance> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrAttendance::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> deptNameByEmp = lookupHelper.deptNameByEmployeeId(empMap.keySet());
        for (HrAttendance a : rows) {
            HrEmployee emp = empMap.get(a.getEmployeeId());
            if (emp != null) {
                a.setEmployeeName(emp.getEmployeeName());
                a.setEmployeeNo(emp.getEmployeeNo());
            }
            a.setDeptName(deptNameByEmp.get(a.getEmployeeId()));
        }
    }

    /** 供请假联动使用：把某段日期标记为休假（已存在的记录改状态，缺失的补建） */
    public List<HrAttendance> markLeave(Long employeeId, LocalDate start, LocalDate end, Long leaveRequestId) {
        List<HrAttendance> created = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            HrAttendance attendance = getOne(new LambdaQueryWrapper<HrAttendance>()
                    .eq(HrAttendance::getEmployeeId, employeeId)
                    .eq(HrAttendance::getAttendanceDate, cursor)
                    .last("limit 1"));
            if (attendance == null) {
                attendance = new HrAttendance()
                        .setEmployeeId(employeeId)
                        .setAttendanceDate(cursor)
                        .setTenantId(SecurityUtils.getCurrentTenantId())
                        .setCreateTime(LocalDateTime.now());
                attendance.setStatus(STATUS_LEAVE);
                attendance.setLeaveRequestId(leaveRequestId);
                attendance.setUpdateTime(LocalDateTime.now());
                save(attendance);
                created.add(attendance);
            } else {
                attendance.setStatus(STATUS_LEAVE);
                attendance.setLeaveRequestId(leaveRequestId);
                attendance.setLateMinutes(0);
                attendance.setEarlyMinutes(0);
                attendance.setUpdateTime(LocalDateTime.now());
                updateById(attendance);
            }
            cursor = cursor.plusDays(1);
        }
        return created;
    }

    /** 供请假撤销/拒绝使用：把该请假单写入的休假标记还原 */
    public void unmarkLeave(Long leaveRequestId) {
        if (leaveRequestId == null) {
            return;
        }
        List<HrAttendance> rows = list(new LambdaQueryWrapper<HrAttendance>()
                .eq(HrAttendance::getLeaveRequestId, leaveRequestId));
        HrAttendanceRule rule = getRule();
        for (HrAttendance a : rows) {
            a.setLeaveRequestId(null);
            a.setStatus(null);
            recompute(a, rule);
            a.setUpdateTime(LocalDateTime.now());
            updateById(a);
        }
    }

}
