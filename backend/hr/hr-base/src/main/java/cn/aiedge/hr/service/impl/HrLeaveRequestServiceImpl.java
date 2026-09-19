package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.attendance.HrLeaveQuota;
import cn.aiedge.hr.attendance.HrLeaveRequest;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrLeaveQuotaMapper;
import cn.aiedge.hr.mapper.HrLeaveRequestMapper;
import cn.aiedge.hr.service.HrAttendanceService;
import cn.aiedge.hr.service.HrLeaveRequestService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 请假申请服务实现
 *
 * <p>关键增强：</p>
 * <ol>
 *   <li><b>申请校验</b>：起止日期、天数与日期区间一致性、假期余额；</li>
 *   <li><b>审批状态机</b>：仅「待审批」可批准/拒绝，已批准只能撤销；</li>
 *   <li><b>请假 ↔ 考勤联动</b>：批准时把请假区间写入考勤的 `LEAVE` 标记（避免休假期间被判缺勤而扣款），
 *       撤销/拒绝时还原。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveRequestServiceImpl extends ServiceImpl<HrLeaveRequestMapper, HrLeaveRequest>
        implements HrLeaveRequestService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_CANCELLED = 3;

    private static final List<String> LEAVE_TYPES =
            Arrays.asList("ANNUAL", "SICK", "PERSONAL", "MATERNITY", "MARRIAGE");

    private final HrLookupHelper lookupHelper;
    private final HrLeaveQuotaMapper quotaMapper;
    private final HrAttendanceService attendanceService;

    @Override
    public Page<HrLeaveRequest> pageRequests(Page<HrLeaveRequest> page, Long tenantId,
                                             Long employeeId, Integer status,
                                             String leaveType, Long deptId,
                                             LocalDate startDateFrom, LocalDate startDateTo) {
        LambdaQueryWrapper<HrLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrLeaveRequest::getTenantId, tenantId)
                .eq(employeeId != null, HrLeaveRequest::getEmployeeId, employeeId)
                .eq(status != null, HrLeaveRequest::getStatus, status)
                .eq(StringUtils.hasText(leaveType), HrLeaveRequest::getLeaveType, leaveType)
                .ge(startDateFrom != null, HrLeaveRequest::getStartDate, startDateFrom)
                .le(startDateTo != null, HrLeaveRequest::getStartDate, startDateTo);
        applyDeptScope(wrapper, deptId);
        wrapper.orderByDesc(HrLeaveRequest::getCreateTime).orderByDesc(HrLeaveRequest::getId);
        Page<HrLeaveRequest> result = page(page, wrapper);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrLeaveRequest> listForExport(Integer status, String leaveType, Long deptId,
                                              LocalDate startDateFrom, LocalDate startDateTo) {
        LambdaQueryWrapper<HrLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, HrLeaveRequest::getStatus, status)
                .eq(StringUtils.hasText(leaveType), HrLeaveRequest::getLeaveType, leaveType)
                .ge(startDateFrom != null, HrLeaveRequest::getStartDate, startDateFrom)
                .le(startDateTo != null, HrLeaveRequest::getStartDate, startDateTo);
        applyDeptScope(wrapper, deptId);
        List<HrLeaveRequest> rows = list(wrapper.orderByDesc(HrLeaveRequest::getCreateTime));
        enrich(rows);
        return rows;
    }

    @Override
    public List<HrLeaveRequest> listByEmployee(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        List<HrLeaveRequest> rows = list(new LambdaQueryWrapper<HrLeaveRequest>()
                .eq(HrLeaveRequest::getEmployeeId, employeeId)
                .orderByDesc(HrLeaveRequest::getStartDate));
        enrich(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRequest(HrLeaveRequest request) {
        validate(request);
        request.setStatus(STATUS_PENDING);
        if (request.getTenantId() == null) {
            request.setTenantId(SecurityUtils.getCurrentTenantId());
        }
        validateBalance(request, 0L);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        save(request);
        log.info("提交请假申请: employeeId={}, type={}, days={}",
                request.getEmployeeId(), request.getLeaveType(), request.getDays());
        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRequest(HrLeaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("请假单ID不能为空");
        }
        HrLeaveRequest exists = getById(request.getId());
        if (exists == null) {
            throw new BusinessException("请假单不存在");
        }
        if (exists.getStatus() == null || exists.getStatus() != STATUS_PENDING) {
            throw new BusinessException("只有「待审批」的请假单可以修改");
        }
        request.setEmployeeId(null);
        request.setStatus(STATUS_PENDING);
        request.setApproveId(null);
        request.setApproveComment(null);
        request.setApproveTime(null);
        // 合并后的最终值用于校验
        HrLeaveRequest merged = new HrLeaveRequest()
                .setEmployeeId(exists.getEmployeeId())
                .setLeaveType(request.getLeaveType() != null ? request.getLeaveType() : exists.getLeaveType())
                .setStartDate(request.getStartDate() != null ? request.getStartDate() : exists.getStartDate())
                .setEndDate(request.getEndDate() != null ? request.getEndDate() : exists.getEndDate())
                .setDays(request.getDays() != null ? request.getDays() : exists.getDays());
        validate(merged);
        validateBalance(merged, exists.getId());
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approveId, String comment) {
        HrLeaveRequest request = require(id);
        if (request.getStatus() == null || request.getStatus() != STATUS_PENDING) {
            throw new BusinessException("只有「待审批」的请假单可以批准");
        }
        LambdaUpdateWrapper<HrLeaveRequest> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrLeaveRequest::getId, id)
                .set(HrLeaveRequest::getStatus, STATUS_APPROVED)
                .set(HrLeaveRequest::getApproveId, approveId)
                .set(HrLeaveRequest::getApproveComment, comment)
                .set(HrLeaveRequest::getApproveTime, LocalDateTime.now())
                .set(HrLeaveRequest::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        // 请假 ↔ 考勤联动：批准即把区间标记为休假
        try {
            attendanceService.markLeave(request.getEmployeeId(), request.getStartDate(), request.getEndDate(), id);
        } catch (Exception e) {
            log.error("写考勤休假标记失败: leaveId={}", id, e);
            throw new BusinessException("请假已批准，但写考勤休假标记失败：" + e.getMessage());
        }
        log.info("批准请假: id={}, employeeId={}", id, request.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, Long approveId, String comment) {
        HrLeaveRequest request = require(id);
        if (request.getStatus() == null || request.getStatus() != STATUS_PENDING) {
            throw new BusinessException("只有「待审批」的请假单可以拒绝");
        }
        LambdaUpdateWrapper<HrLeaveRequest> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrLeaveRequest::getId, id)
                .set(HrLeaveRequest::getStatus, STATUS_REJECTED)
                .set(HrLeaveRequest::getApproveId, approveId)
                .set(HrLeaveRequest::getApproveComment, comment)
                .set(HrLeaveRequest::getApproveTime, LocalDateTime.now())
                .set(HrLeaveRequest::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        attendanceService.unmarkLeave(id);
        log.info("拒绝请假: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        HrLeaveRequest request = require(id);
        if (request.getStatus() == null
                || (request.getStatus() != STATUS_PENDING && request.getStatus() != STATUS_APPROVED)) {
            throw new BusinessException("已拒绝或已撤销的请假单不能再次撤销");
        }
        LambdaUpdateWrapper<HrLeaveRequest> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrLeaveRequest::getId, id)
                .set(HrLeaveRequest::getStatus, STATUS_CANCELLED)
                .set(HrLeaveRequest::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        // 已批准过的撤销要还原考勤休假标记
        if (request.getStatus() == STATUS_APPROVED) {
            attendanceService.unmarkLeave(id);
        }
        log.info("撤销请假: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRequest(Long id) {
        require(id);
        removeById(id);
    }

    @Override
    public Map<String, Object> balance(Long employeeId, Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        List<HrLeaveQuota> quotas = listQuotas(targetYear);
        Map<String, BigDecimal> used = usedDaysByType(employeeId, targetYear);

        List<Map<String, Object>> items = new ArrayList<>();
        for (String type : LEAVE_TYPES) {
            HrLeaveQuota quota = quotas.stream()
                    .filter(q -> type.equals(q.getLeaveType())).findFirst().orElse(null);
            BigDecimal quotaDays = quota == null || quota.getQuotaDays() == null
                    ? BigDecimal.ZERO : quota.getQuotaDays();
            BigDecimal usedDays = used.getOrDefault(type, BigDecimal.ZERO);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("leaveType", type);
            item.put("quotaDays", quotaDays);
            item.put("usedDays", usedDays);
            item.put("remainingDays", quotaDays.subtract(usedDays));
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId", employeeId);
        result.put("year", targetYear);
        result.put("items", items);
        return result;
    }

    /**
     * 年度额度清单。
     *
     * <p>**始终返回全部 5 种请假类型**（未配置的补 0，`id` 为 null）：额度配置弹窗要能一次列出
     * 所有类型让用户逐个填写；若只返回库里已有的行，首次进入时弹窗会是空的，用户无从配置。</p>
     */
    @Override
    public List<HrLeaveQuota> listQuotas(Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        List<HrLeaveQuota> stored = quotaMapper.selectList(new LambdaQueryWrapper<HrLeaveQuota>()
                .eq(HrLeaveQuota::getYear, targetYear));
        List<HrLeaveQuota> result = new ArrayList<>();
        for (String type : LEAVE_TYPES) {
            HrLeaveQuota hit = stored.stream()
                    .filter(q -> type.equals(q.getLeaveType())).findFirst().orElse(null);
            if (hit != null) {
                result.add(hit);
            } else {
                result.add(new HrLeaveQuota()
                        .setLeaveType(type)
                        .setYear(targetYear)
                        .setQuotaDays(BigDecimal.ZERO));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrLeaveQuota saveQuota(HrLeaveQuota quota) {
        if (!StringUtils.hasText(quota.getLeaveType())) {
            throw new BusinessException("请选择请假类型");
        }
        if (quota.getYear() == null) {
            quota.setYear(LocalDate.now().getYear());
        }
        HrLeaveQuota exists = quotaMapper.selectOne(new LambdaQueryWrapper<HrLeaveQuota>()
                .eq(HrLeaveQuota::getLeaveType, quota.getLeaveType())
                .eq(HrLeaveQuota::getYear, quota.getYear())
                .last("limit 1"));
        if (exists == null) {
            quota.setTenantId(SecurityUtils.getCurrentTenantId());
            quota.setCreateTime(LocalDateTime.now());
            quota.setUpdateTime(LocalDateTime.now());
            quotaMapper.insert(quota);
            return quota;
        }
        exists.setQuotaDays(quota.getQuotaDays());
        if (quota.getRemark() != null) {
            exists.setRemark(quota.getRemark());
        }
        exists.setUpdateTime(LocalDateTime.now());
        quotaMapper.updateById(exists);
        return exists;
    }

    @Override
    public Map<String, Object> statistics(Integer year, Long deptId) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LambdaQueryWrapper<HrLeaveRequest> wrapper = new LambdaQueryWrapper<HrLeaveRequest>()
                .ge(HrLeaveRequest::getStartDate, LocalDate.of(targetYear, 1, 1))
                .le(HrLeaveRequest::getStartDate, LocalDate.of(targetYear, 12, 31));
        applyDeptScope(wrapper, deptId);
        List<HrLeaveRequest> rows = list(wrapper);
        long pending = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_PENDING).count();
        long approved = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_APPROVED).count();
        long rejected = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == STATUS_REJECTED).count();
        BigDecimal approvedDays = rows.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == STATUS_APPROVED)
                .map(HrLeaveRequest::getDays).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("year", targetYear);
        stat.put("total", rows.size());
        stat.put("pendingCount", pending);
        stat.put("approvedCount", approved);
        stat.put("rejectedCount", rejected);
        stat.put("approvedDays", approvedDays);
        return stat;
    }

    // ── 内部 ──────────────────────────────────────────────

    private void applyDeptScope(LambdaQueryWrapper<HrLeaveRequest> wrapper, Long deptId) {
        if (deptId == null) {
            return;
        }
        List<Long> ids = lookupHelper.employeeIdsOfDept(deptId);
        if (ids.isEmpty()) {
            wrapper.eq(HrLeaveRequest::getId, -1L);
        } else {
            wrapper.in(HrLeaveRequest::getEmployeeId, ids);
        }
    }

    private HrLeaveRequest require(Long id) {
        HrLeaveRequest request = id == null ? null : getById(id);
        if (request == null) {
            throw new BusinessException("请假单不存在");
        }
        return request;
    }

    private void validate(HrLeaveRequest request) {
        if (request.getEmployeeId() == null) {
            throw new BusinessException("请选择请假员工");
        }
        if (!StringUtils.hasText(request.getLeaveType())) {
            throw new BusinessException("请选择请假类型");
        }
        if (!LEAVE_TYPES.contains(request.getLeaveType())) {
            throw new BusinessException("非法的请假类型：" + request.getLeaveType());
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException("请填写请假起止日期");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("请假开始日期不能晚于结束日期");
        }
        if (request.getDays() == null || request.getDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请假天数必须大于 0");
        }
    }

    /** 余额校验：额度为 0（未配置）时不拦截，配置了才校验，避免"未配置即无法请假" */
    private void validateBalance(HrLeaveRequest request, Long excludeId) {
        int year = request.getStartDate().getYear();
        HrLeaveQuota quota = quotaMapper.selectOne(new LambdaQueryWrapper<HrLeaveQuota>()
                .eq(HrLeaveQuota::getLeaveType, request.getLeaveType())
                .eq(HrLeaveQuota::getYear, year)
                .last("limit 1"));
        if (quota == null || quota.getQuotaDays() == null || quota.getQuotaDays().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal used = usedDaysByType(request.getEmployeeId(), year, excludeId)
                .getOrDefault(request.getLeaveType(), BigDecimal.ZERO);
        BigDecimal remaining = quota.getQuotaDays().subtract(used);
        if (request.getDays().compareTo(remaining) > 0) {
            throw new BusinessException(String.format(
                    "%s 年度余额不足：额度 %s 天，已用 %s 天，剩余 %s 天，本次申请 %s 天",
                    year, quota.getQuotaDays().stripTrailingZeros().toPlainString(),
                    used.stripTrailingZeros().toPlainString(),
                    remaining.stripTrailingZeros().toPlainString(),
                    request.getDays().stripTrailingZeros().toPlainString()));
        }
    }

    private Map<String, BigDecimal> usedDaysByType(Long employeeId, int year) {
        return usedDaysByType(employeeId, year, null);
    }

    private Map<String, BigDecimal> usedDaysByType(Long employeeId, int year, Long excludeId) {
        if (employeeId == null) {
            return Map.of();
        }
        List<HrLeaveRequest> approved = list(new LambdaQueryWrapper<HrLeaveRequest>()
                .eq(HrLeaveRequest::getEmployeeId, employeeId)
                .eq(HrLeaveRequest::getStatus, STATUS_APPROVED)
                .ne(excludeId != null, HrLeaveRequest::getId, excludeId)
                .ge(HrLeaveRequest::getStartDate, LocalDate.of(year, 1, 1))
                .le(HrLeaveRequest::getStartDate, LocalDate.of(year, 12, 31)));
        return approved.stream()
                .filter(r -> StringUtils.hasText(r.getLeaveType()) && r.getDays() != null)
                .collect(Collectors.toMap(HrLeaveRequest::getLeaveType, HrLeaveRequest::getDays, BigDecimal::add));
    }

    private void enrich(List<HrLeaveRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrLeaveRequest::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> deptNameByEmp = lookupHelper.deptNameByEmployeeId(empMap.keySet());
        for (HrLeaveRequest r : rows) {
            HrEmployee emp = empMap.get(r.getEmployeeId());
            if (emp != null) {
                r.setEmployeeName(emp.getEmployeeName());
                r.setEmployeeNo(emp.getEmployeeNo());
            }
            r.setDeptName(deptNameByEmp.get(r.getEmployeeId()));
        }
    }
}
