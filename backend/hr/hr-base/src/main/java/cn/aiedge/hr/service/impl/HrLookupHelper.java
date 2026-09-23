package cn.aiedge.hr.service.impl;

import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrDepartmentRefMapper;
import cn.aiedge.hr.mapper.HrEmployeeMapper;
import cn.aiedge.hr.mapper.HrPositionMapper;
import cn.aiedge.hr.organization.HrPosition;
import cn.aiedge.hr.ref.HrDepartmentRef;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HR 台账联表回填助手
 *
 * <p>各 HR 列表的实体里带了 `@TableField(exist = false)` 的展示字段（员工姓名/工号/部门名/岗位名），
 * 由本助手统一批量回填，避免每页 N+1 查询。所有查询都走 BaseMapper，
 * 因此多租户拦截器与逻辑删除均正常生效。</p>
 */
@Component
@RequiredArgsConstructor
public class HrLookupHelper {

    private final HrEmployeeMapper employeeMapper;
    private final HrPositionMapper positionMapper;
    private final HrDepartmentRefMapper departmentRefMapper;

    /** 按 id 批量取部门名称 */
    public Map<Long, String> deptNames(Collection<Long> deptIds) {
        Set<Long> ids = clean(deptIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrDepartmentRef> rows = departmentRefMapper.selectList(
                new LambdaQueryWrapper<HrDepartmentRef>().in(HrDepartmentRef::getId, ids));
        return rows.stream()
                .filter(d -> d.getId() != null && d.getDeptName() != null)
                .collect(Collectors.toMap(HrDepartmentRef::getId, HrDepartmentRef::getDeptName, (a, b) -> a));
    }

    /** 按 id 批量取岗位名称 */
    public Map<Long, String> positionNames(Collection<Long> positionIds) {
        Set<Long> ids = clean(positionIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrPosition> rows = positionMapper.selectList(
                new LambdaQueryWrapper<HrPosition>().in(HrPosition::getId, ids));
        return rows.stream()
                .filter(p -> p.getId() != null && p.getPositionName() != null)
                .collect(Collectors.toMap(HrPosition::getId, HrPosition::getPositionName, (a, b) -> a));
    }

    /** 按 id 批量取员工（含部门/岗位名回填） */
    public Map<Long, HrEmployee> employees(Collection<Long> employeeIds) {
        Set<Long> ids = clean(employeeIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<HrEmployee> rows = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>().in(HrEmployee::getId, ids));
        Map<Long, HrEmployee> map = new HashMap<>();
        for (HrEmployee e : rows) {
            map.put(e.getId(), e);
        }
        return map;
    }

    /** 按部门取员工 id 列表（用于子表按部门筛选） */
    public List<Long> employeeIdsOfDept(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return employeeMapper.selectList(
                        new LambdaQueryWrapper<HrEmployee>()
                                .select(HrEmployee::getId)
                                .eq(HrEmployee::getDeptId, deptId))
                .stream().map(HrEmployee::getId).collect(Collectors.toList());
    }

    /** 按关键字（工号 / 姓名 / 手机号）取员工 id 列表（用于子表按员工关键字筛选） */
    public List<Long> employeeIdsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();
        return employeeMapper.selectList(
                        new LambdaQueryWrapper<HrEmployee>()
                                .select(HrEmployee::getId)
                                .and(w -> w.like(HrEmployee::getEmployeeNo, kw)
                                        .or().like(HrEmployee::getEmployeeName, kw)
                                        .or().like(HrEmployee::getPhone, kw)))
                .stream().map(HrEmployee::getId).collect(Collectors.toList());
    }

    /** 建立「员工 id → 部门名」映射 */
    public Map<Long, String> deptNameByEmployeeId(Collection<Long> employeeIds) {
        Map<Long, HrEmployee> empMap = employees(employeeIds);
        if (empMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> deptNameMap = deptNames(empMap.values().stream()
                .map(HrEmployee::getDeptId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> result = new HashMap<>();
        empMap.forEach((id, emp) -> result.put(id, emp.getDeptId() == null ? null : deptNameMap.get(emp.getDeptId())));
        return result;
    }

    private Set<Long> clean(Collection<Long> ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                set.add(id);
            }
        }
        return set;
    }
}
