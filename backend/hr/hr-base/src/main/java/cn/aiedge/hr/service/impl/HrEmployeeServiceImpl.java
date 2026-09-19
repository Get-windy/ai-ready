package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.hr.change.HrEmployeeChange;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrEmployeeChangeMapper;
import cn.aiedge.hr.mapper.HrEmployeeMapper;
import cn.aiedge.hr.service.HrEmployeeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 员工档案服务实现
 *
 * <p>本实现承担三件过去缺失的事：</p>
 * <ol>
 *   <li><b>工号生成</b>：走全站号段体系（bizType = <code>EMP</code>），租户内唯一，杜绝「空工号第二条撞唯一约束」；</li>
 *   <li><b>状态机</b>：试用(2) → 在职(1) → 离职(0)，非法迁移（如 0 → 1）一律拒绝；</li>
 *   <li><b>异动留痕</b>：建档/转正/调岗/调薪/离职自动写 <code>hr_employee_change</code>（含前后快照），
 *       并同步维护 <code>hr_position.current_count</code>（编制在岗人数）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrEmployeeServiceImpl extends ServiceImpl<HrEmployeeMapper, HrEmployee>
        implements HrEmployeeService {

    /** 状态：离职 / 在职 / 试用 */
    private static final int STATUS_RESIGNED = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_PROBATION = 2;

    private final HrEmployeeChangeMapper changeMapper;
    private final HrLookupHelper lookupHelper;
    private final HrPositionServiceImpl positionService;
    private final BizNumberGeneratorService numberGenerator;

    @Override
    public Page<HrEmployee> pageEmployees(Page<HrEmployee> page, HrEmployeeQuery query) {
        Page<HrEmployee> result = page(page, buildWrapper(query));
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrEmployee> listForExport(HrEmployeeQuery query) {
        List<HrEmployee> rows = list(buildWrapper(query));
        enrich(rows);
        return rows;
    }

    private LambdaQueryWrapper<HrEmployee> buildWrapper(HrEmployeeQuery query) {
        HrEmployeeQuery q = query == null ? new HrEmployeeQuery() : query;
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(q.getTenantId() != null, HrEmployee::getTenantId, q.getTenantId())
                .eq(q.getDeptId() != null, HrEmployee::getDeptId, q.getDeptId())
                .eq(q.getPositionId() != null, HrEmployee::getPositionId, q.getPositionId())
                .eq(q.getStatus() != null, HrEmployee::getStatus, q.getStatus())
                .eq(q.getEmployeeType() != null, HrEmployee::getEmployeeType, q.getEmployeeType())
                .eq(q.getGender() != null, HrEmployee::getGender, q.getGender())
                .eq(q.getEducation() != null, HrEmployee::getEducation, q.getEducation())
                .like(StringUtils.hasText(q.getEmployeeName()), HrEmployee::getEmployeeName, q.getEmployeeName())
                .like(StringUtils.hasText(q.getEmployeeNo()), HrEmployee::getEmployeeNo, q.getEmployeeNo())
                .like(StringUtils.hasText(q.getPhone()), HrEmployee::getPhone, q.getPhone())
                .ge(q.getHireDateStart() != null, HrEmployee::getHireDate, q.getHireDateStart())
                .le(q.getHireDateEnd() != null, HrEmployee::getHireDate, q.getHireDateEnd())
                .ge(q.getLeaveDateStart() != null, HrEmployee::getLeaveDate, q.getLeaveDateStart())
                .le(q.getLeaveDateEnd() != null, HrEmployee::getLeaveDate, q.getLeaveDateEnd());
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(HrEmployee::getEmployeeNo, kw)
                    .or().like(HrEmployee::getEmployeeName, kw)
                    .or().like(HrEmployee::getPhone, kw));
        }
        applySort(wrapper, q.getSortField(), q.getSortOrder());
        return wrapper;
    }

    /** 服务端排序白名单：未在白名单内的字段一律回落「创建时间倒序」，防止注入任意列名 */
    private void applySort(LambdaQueryWrapper<HrEmployee> wrapper, String sortField, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (StringUtils.hasText(sortField)) {
            switch (sortField) {
                case "employeeNo" -> wrapper.orderBy(true, asc, HrEmployee::getEmployeeNo);
                case "employeeName" -> wrapper.orderBy(true, asc, HrEmployee::getEmployeeName);
                case "hireDate" -> wrapper.orderBy(true, asc, HrEmployee::getHireDate);
                case "status" -> wrapper.orderBy(true, asc, HrEmployee::getStatus);
                case "createTime" -> wrapper.orderBy(true, asc, HrEmployee::getCreateTime);
                default -> wrapper.orderByDesc(HrEmployee::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(HrEmployee::getCreateTime);
        }
        wrapper.orderByDesc(HrEmployee::getId);
    }

    @Override
    public Page<HrEmployeeChange> pageChanges(Page<HrEmployeeChange> page, Long employeeId,
                                              String changeType, LocalDate dateFrom, LocalDate dateTo) {
        LambdaQueryWrapper<HrEmployeeChange> wrapper = new LambdaQueryWrapper<HrEmployeeChange>()
                .eq(employeeId != null, HrEmployeeChange::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(changeType), HrEmployeeChange::getChangeType, changeType)
                .ge(dateFrom != null, HrEmployeeChange::getEffectiveDate, dateFrom)
                .le(dateTo != null, HrEmployeeChange::getEffectiveDate, dateTo)
                .orderByDesc(HrEmployeeChange::getEffectiveDate)
                .orderByDesc(HrEmployeeChange::getId);
        return changeMapper.selectPage(page, wrapper);
    }

    @Override
    public HrEmployee getByEmployeeNo(String employeeNo) {
        if (!StringUtils.hasText(employeeNo)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<HrEmployee>().eq(HrEmployee::getEmployeeNo, employeeNo).last("limit 1"));
    }

    @Override
    public String nextEmployeeNo() {
        return numberGenerator.nextNumber("EMP", SecurityUtils.getCurrentTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEmployee(HrEmployee employee) {
        if (!StringUtils.hasText(employee.getEmployeeName())) {
            throw new BusinessException("员工姓名不能为空");
        }
        // 工号：未填则号段生成；填了则查重
        if (!StringUtils.hasText(employee.getEmployeeNo())) {
            employee.setEmployeeNo(nextEmployeeNo());
        } else if (existsEmployeeNo(employee.getEmployeeNo(), null)) {
            throw new BusinessException("员工工号已存在：" + employee.getEmployeeNo());
        }
        // 新建一律进入试用期（与旧实现口径一致）
        employee.setStatus(STATUS_PROBATION);
        if (employee.getHireDate() == null) {
            employee.setHireDate(LocalDate.now());
        }
        if (employee.getEmployeeType() == null) {
            employee.setEmployeeType(1);
        }
        employee.setTenantId(SecurityUtils.getCurrentTenantId());
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        save(employee);

        writeChange(employee, "ENTRY", employee.getHireDate(), null, employee, "新建员工档案");
        positionService.refreshCurrentCount(employee.getPositionId());
        log.info("创建员工: employeeNo={}, name={}", employee.getEmployeeNo(), employee.getEmployeeName());
        return employee.getId();
    }

    /**
     * 更新员工主档。
     *
     * <p><b>字段白名单</b>：`status` / `leaveDate` / `regularDate` 等状态类字段**不接受**本端点直改
     * （须走转正 / 离职 / 复职动作），否则会出现「表单顺手把离职员工改回在职」这类越权状态迁移。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(HrEmployee employee) {
        if (employee.getId() == null) {
            throw new BusinessException("员工ID不能为空");
        }
        HrEmployee before = getById(employee.getId());
        if (before == null) {
            throw new BusinessException("员工不存在");
        }
        if (StringUtils.hasText(employee.getEmployeeNo())
                && existsEmployeeNo(employee.getEmployeeNo(), employee.getId())) {
            throw new BusinessException("员工工号已存在：" + employee.getEmployeeNo());
        }
        LambdaUpdateWrapper<HrEmployee> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrEmployee::getId, employee.getId());
        setIfPresent(wrapper, HrEmployee::getEmployeeName, employee.getEmployeeName(), true);
        setIfPresent(wrapper, HrEmployee::getEmployeeNo, employee.getEmployeeNo(), false);
        wrapper.set(HrEmployee::getDeptId, employee.getDeptId());
        wrapper.set(HrEmployee::getPositionId, employee.getPositionId());
        setIfPresent(wrapper, HrEmployee::getGender, employee.getGender(), false);
        wrapper.set(HrEmployee::getBirthDate, employee.getBirthDate());
        setIfPresent(wrapper, HrEmployee::getPhone, employee.getPhone(), false);
        setIfPresent(wrapper, HrEmployee::getEmail, employee.getEmail(), false);
        setIfPresent(wrapper, HrEmployee::getIdCard, employee.getIdCard(), false);
        setIfPresent(wrapper, HrEmployee::getEducation, employee.getEducation(), false);
        setIfPresent(wrapper, HrEmployee::getSchool, employee.getSchool(), false);
        setIfPresent(wrapper, HrEmployee::getMajor, employee.getMajor(), false);
        wrapper.set(HrEmployee::getHireDate, employee.getHireDate());
        setIfPresent(wrapper, HrEmployee::getEmployeeType, employee.getEmployeeType(), false);
        setIfPresent(wrapper, HrEmployee::getAvatarUrl, employee.getAvatarUrl(), false);
        setIfPresent(wrapper, HrEmployee::getEmergencyContact, employee.getEmergencyContact(), false);
        setIfPresent(wrapper, HrEmployee::getEmergencyPhone, employee.getEmergencyPhone(), false);
        setIfPresent(wrapper, HrEmployee::getHometownAddress, employee.getHometownAddress(), false);
        setIfPresent(wrapper, HrEmployee::getCurrentAddress, employee.getCurrentAddress(), false);
        setIfPresent(wrapper, HrEmployee::getRemark, employee.getRemark(), false);
        wrapper.set(HrEmployee::getUpdateTime, LocalDateTime.now());
        update(wrapper);

        HrEmployee after = getById(employee.getId());
        String changeType = resolveChangeType(before, after);
        if (changeType != null) {
            writeChange(after, changeType, LocalDate.now(), before, after, null);
        }
        // 调岗会同时影响新旧两个岗位的在岗人数
        if (!Objects.equals(before.getPositionId(), after.getPositionId())) {
            positionService.refreshCurrentCount(before.getPositionId());
            positionService.refreshCurrentCount(after.getPositionId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void regularize(Long id, LocalDate regularDate, String remark) {
        HrEmployee before = requireEmployee(id);
        if (before.getStatus() == null || before.getStatus() != STATUS_PROBATION) {
            throw new BusinessException("只有「试用」状态的员工可以转正，当前状态不允许该操作");
        }
        LocalDate effective = regularDate == null ? LocalDate.now() : regularDate;
        LambdaUpdateWrapper<HrEmployee> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrEmployee::getId, id)
                .set(HrEmployee::getStatus, STATUS_ACTIVE)
                .set(HrEmployee::getRegularDate, effective)
                .set(HrEmployee::getUpdateTime, LocalDateTime.now());
        update(wrapper);
        HrEmployee after = getById(id);
        writeChange(after, "REGULAR", effective, before, after,
                StringUtils.hasText(remark) ? remark : "试用期转正");
        log.info("员工转正: id={}, regularDate={}", id, effective);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resign(Long id, LocalDate lastWorkDate, Integer resignType, String resignReason) {
        HrEmployee before = requireEmployee(id);
        if (before.getStatus() == null || before.getStatus() == STATUS_RESIGNED) {
            throw new BusinessException("该员工已是离职状态，不能重复办理离职");
        }
        LocalDate effective = lastWorkDate == null ? LocalDate.now() : lastWorkDate;
        LambdaUpdateWrapper<HrEmployee> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HrEmployee::getId, id)
                .set(HrEmployee::getStatus, STATUS_RESIGNED)
                .set(HrEmployee::getLeaveDate, effective)
                .set(HrEmployee::getUpdateTime, LocalDateTime.now());
        if (resignType != null) {
            wrapper.set(HrEmployee::getResignType, resignType);
        }
        if (StringUtils.hasText(resignReason)) {
            wrapper.set(HrEmployee::getResignReason, resignReason);
        }
        update(wrapper);
        HrEmployee after = getById(id);
        writeChange(after, "RESIGN", effective, before, after, resignReason);
        positionService.refreshCurrentCount(after.getPositionId());
        log.info("员工离职: id={}, lastWorkDate={}", id, effective);
    }

    /**
     * 状态流转（兼容旧端点）。
     *
     * <p>合法迁移矩阵：试用(2) → 在职(1)、在职(1) → 离职(0)、试用(2) → 离职(0)。
     * 离职(0) 是终态——复职须走专门动作，不允许经本端点直接把 0 改成 1。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        HrEmployee before = requireEmployee(id);
        int current = before.getStatus() == null ? STATUS_RESIGNED : before.getStatus();
        if (status == STATUS_ACTIVE && current == STATUS_PROBATION) {
            regularize(id, LocalDate.now(), null);
            return;
        }
        if (status == STATUS_RESIGNED && (current == STATUS_ACTIVE || current == STATUS_PROBATION)) {
            resign(id, LocalDate.now(), null, null);
            return;
        }
        if (current == STATUS_RESIGNED && status != STATUS_RESIGNED) {
            throw new BusinessException("离职员工为终态，请使用「复职」动作恢复在职");
        }
        throw new BusinessException("非法的状态迁移：" + current + " → " + status);
    }

    @Override
    public List<HrEmployee> getByDeptId(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }
        List<HrEmployee> rows = list(new LambdaQueryWrapper<HrEmployee>().eq(HrEmployee::getDeptId, deptId));
        enrich(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeEmployee(Long id) {
        HrEmployee employee = requireEmployee(id);
        removeById(id);
        positionService.refreshCurrentCount(employee.getPositionId());
        log.info("删除员工: id={}, employeeNo={}", id, employee.getEmployeeNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的员工");
        }
        List<HrEmployee> employees = listByIds(ids);
        removeByIds(ids);
        employees.stream().map(HrEmployee::getPositionId).filter(Objects::nonNull).distinct()
                .forEach(positionService::refreshCurrentCount);
        log.info("批量删除员工: count={}", employees.size());
    }

    @Override
    public Map<String, Object> statistics(Long deptId) {
        long total = count(new LambdaQueryWrapper<HrEmployee>()
                .eq(deptId != null, HrEmployee::getDeptId, deptId));
        long active = count(new LambdaQueryWrapper<HrEmployee>()
                .eq(deptId != null, HrEmployee::getDeptId, deptId).eq(HrEmployee::getStatus, STATUS_ACTIVE));
        long probation = count(new LambdaQueryWrapper<HrEmployee>()
                .eq(deptId != null, HrEmployee::getDeptId, deptId).eq(HrEmployee::getStatus, STATUS_PROBATION));
        long resigned = count(new LambdaQueryWrapper<HrEmployee>()
                .eq(deptId != null, HrEmployee::getDeptId, deptId).eq(HrEmployee::getStatus, STATUS_RESIGNED));
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long hiredThisMonth = count(new LambdaQueryWrapper<HrEmployee>()
                .eq(deptId != null, HrEmployee::getDeptId, deptId)
                .ge(HrEmployee::getHireDate, monthStart));

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", total);
        stat.put("active", active);
        stat.put("probation", probation);
        stat.put("resigned", resigned);
        stat.put("onDuty", active + probation);
        stat.put("hiredThisMonth", hiredThisMonth);
        return stat;
    }

    /** 按变更内容推断异动类型（部门变=调岗，岗位变=调岗，其余=信息变更） */
    private String resolveChangeType(HrEmployee before, HrEmployee after) {
        if (before == null || after == null) {
            return null;
        }
        if (!Objects.equals(before.getDeptId(), after.getDeptId())
                || !Objects.equals(before.getPositionId(), after.getPositionId())) {
            return "TRANSFER";
        }
        return "UPDATE";
    }

    private boolean existsEmployeeNo(String employeeNo, Long excludeId) {
        Long c = baseMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getEmployeeNo, employeeNo)
                .ne(excludeId != null, HrEmployee::getId, excludeId));
        return c != null && c > 0;
    }

    private HrEmployee requireEmployee(Long id) {
        HrEmployee employee = id == null ? null : getById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        return employee;
    }

    /** 写入异动快照（before/after 用轻量 JSON，只保留关键字段） */
    private void writeChange(HrEmployee employee, String changeType, LocalDate effectiveDate,
                             HrEmployee before, HrEmployee after, String reason) {
        try {
            HrEmployeeChange change = new HrEmployeeChange();
            change.setTenantId(employee.getTenantId());
            change.setEmployeeId(employee.getId());
            change.setEmployeeNo(employee.getEmployeeNo());
            change.setEmployeeName(employee.getEmployeeName());
            change.setChangeType(changeType);
            change.setEffectiveDate(effectiveDate == null ? LocalDate.now() : effectiveDate);
            change.setBeforeJson(snapshot(before));
            change.setAfterJson(snapshot(after));
            change.setReason(reason);
            change.setOperatorId(SecurityUtils.getCurrentUserId());
            change.setOperatorName(SecurityUtils.getCurrentUsername());
            change.setCreateTime(LocalDateTime.now());
            change.setUpdateTime(LocalDateTime.now());
            changeMapper.insert(change);
        } catch (Exception e) {
            // 异动留痕失败不应阻断主流程，但必须留日志（避免"静默丢历史"）
            log.error("写人事异动记录失败: employeeId={}, type={}", employee.getId(), changeType, e);
        }
    }

    private String snapshot(HrEmployee e) {
        if (e == null) {
            return null;
        }
        return String.format(
                "{\"employeeNo\":%s,\"employeeName\":%s,\"deptId\":%s,\"positionId\":%s,\"status\":%s,"
                        + "\"employeeType\":%s,\"hireDate\":%s,\"leaveDate\":%s}",
                json(e.getEmployeeNo()), json(e.getEmployeeName()), e.getDeptId(), e.getPositionId(),
                e.getStatus(), e.getEmployeeType(), json(e.getHireDate()), json(e.getLeaveDate()));
    }

    private String json(Object v) {
        if (v == null) {
            return "null";
        }
        String s = String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }

    private <T> void setIfPresent(LambdaUpdateWrapper<HrEmployee> wrapper,
                                  com.baomidou.mybatisplus.core.toolkit.support.SFunction<HrEmployee, T> column,
                                  T value, boolean required) {
        if (required) {
            if (value == null || (value instanceof String s && !StringUtils.hasText(s))) {
                throw new BusinessException("必填字段不能为空");
            }
        }
        if (value != null) {
            wrapper.set(column, value);
        }
    }

    /** 回填部门名/岗位名/工龄 */
    private void enrich(List<HrEmployee> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, String> deptNames = lookupHelper.deptNames(rows.stream()
                .map(HrEmployee::getDeptId).filter(Objects::nonNull).collect(Collectors.toList()));
        Map<Long, String> positionNames = lookupHelper.positionNames(rows.stream()
                .map(HrEmployee::getPositionId).filter(Objects::nonNull).collect(Collectors.toList()));
        LocalDate today = LocalDate.now();
        for (HrEmployee e : rows) {
            e.setDeptName(e.getDeptId() == null ? null : deptNames.get(e.getDeptId()));
            e.setPositionName(e.getPositionId() == null ? null : positionNames.get(e.getPositionId()));
            if (e.getHireDate() != null) {
                LocalDate end = e.getLeaveDate() != null ? e.getLeaveDate() : today;
                int years = Period.between(e.getHireDate(), end).getYears();
                e.setWorkYears(java.math.BigDecimal.valueOf(Math.max(years, 0)));
            }
        }
    }
}
