package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.salary.HrSalaryStructure;
import cn.aiedge.hr.mapper.HrSalaryStructureMapper;
import cn.aiedge.hr.service.HrSalaryStructureService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 员工薪资结构服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrSalaryStructureServiceImpl extends ServiceImpl<HrSalaryStructureMapper, HrSalaryStructure>
        implements HrSalaryStructureService {

    private final HrLookupHelper lookupHelper;

    @Override
    public Page<HrSalaryStructure> pageStructures(Page<HrSalaryStructure> page, Long tenantId,
                                                   Long employeeId, Integer status) {
        Page<HrSalaryStructure> result = page(page, new LambdaQueryWrapper<HrSalaryStructure>()
                .eq(tenantId != null, HrSalaryStructure::getTenantId, tenantId)
                .eq(employeeId != null, HrSalaryStructure::getEmployeeId, employeeId)
                .eq(status != null, HrSalaryStructure::getStatus, status)
                .orderByDesc(HrSalaryStructure::getEffectiveDate)
                .orderByDesc(HrSalaryStructure::getId));
        enrich(result.getRecords());
        return result;
    }

    @Override
    public HrSalaryStructure getEffectiveByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        // 生效口径：status=1（生效）且生效日期不晚于今天，取生效日期最近的一条
        HrSalaryStructure structure = getOne(new LambdaQueryWrapper<HrSalaryStructure>()
                .eq(HrSalaryStructure::getEmployeeId, employeeId)
                .eq(HrSalaryStructure::getStatus, 1)
                .le(HrSalaryStructure::getEffectiveDate, LocalDate.now())
                .orderByDesc(HrSalaryStructure::getEffectiveDate)
                .orderByDesc(HrSalaryStructure::getId)
                .last("limit 1"));
        if (structure == null) {
            // 兜底：没有任何满足生效日期的记录时，取最新一条（避免"配了结构却查不到"）
            structure = getOne(new LambdaQueryWrapper<HrSalaryStructure>()
                    .eq(HrSalaryStructure::getEmployeeId, employeeId)
                    .eq(HrSalaryStructure::getStatus, 1)
                    .orderByDesc(HrSalaryStructure::getEffectiveDate)
                    .orderByDesc(HrSalaryStructure::getId)
                    .last("limit 1"));
        }
        return structure;
    }

    @Override
    public List<HrSalaryStructure> listByEmployee(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        List<HrSalaryStructure> rows = list(new LambdaQueryWrapper<HrSalaryStructure>()
                .eq(HrSalaryStructure::getEmployeeId, employeeId)
                .orderByDesc(HrSalaryStructure::getEffectiveDate));
        enrich(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStructure(HrSalaryStructure structure) {
        if (structure.getEmployeeId() == null) {
            throw new BusinessException("请选择员工");
        }
        if (structure.getEffectiveDate() == null) {
            structure.setEffectiveDate(LocalDate.now());
        }
        structure.setStatus(1);
        structure.setTenantId(SecurityUtils.getCurrentTenantId());
        structure.setCreateTime(LocalDateTime.now());
        structure.setUpdateTime(LocalDateTime.now());
        save(structure);
        // 新结构生效后，同员工其它结构置为失效，保证「生效中」唯一
        expireOthers(structure.getEmployeeId(), structure.getId(), structure.getEffectiveDate());
        log.info("创建薪资结构: employeeId={}, effectiveDate={}", structure.getEmployeeId(), structure.getEffectiveDate());
        return structure.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStructure(HrSalaryStructure structure) {
        if (structure.getId() == null) {
            throw new BusinessException("薪资结构ID不能为空");
        }
        HrSalaryStructure exists = getById(structure.getId());
        if (exists == null) {
            throw new BusinessException("薪资结构不存在");
        }
        structure.setEmployeeId(null);
        structure.setUpdateTime(LocalDateTime.now());
        updateById(structure);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStructure(Long id) {
        if (id == null || getById(id) == null) {
            throw new BusinessException("薪资结构不存在");
        }
        removeById(id);
    }

    /** 同一员工只保留一条「生效中」结构，其余置失效（避免月度生成取到旧结构） */
    private void expireOthers(Long employeeId, Long keepId, LocalDate effectiveDate) {
        List<HrSalaryStructure> others = list(new LambdaQueryWrapper<HrSalaryStructure>()
                .eq(HrSalaryStructure::getEmployeeId, employeeId)
                .ne(HrSalaryStructure::getId, keepId)
                .eq(HrSalaryStructure::getStatus, 1)
                .le(HrSalaryStructure::getEffectiveDate, effectiveDate));
        for (HrSalaryStructure other : others) {
            HrSalaryStructure update = new HrSalaryStructure();
            update.setId(other.getId());
            update.setStatus(0);
            update.setExpiryDate(effectiveDate.minusDays(1));
            update.setUpdateTime(LocalDateTime.now());
            updateById(update);
        }
    }

    private void enrich(List<HrSalaryStructure> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrSalaryStructure::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        for (HrSalaryStructure s : rows) {
            HrEmployee emp = empMap.get(s.getEmployeeId());
            if (emp != null) {
                s.setEmployeeName(emp.getEmployeeName());
                s.setEmployeeNo(emp.getEmployeeNo());
            }
        }
        // 部门名单独回填
        Map<Long, String> deptNameByEmp = lookupHelper.deptNameByEmployeeId(rows.stream()
                .map(HrSalaryStructure::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toList()));
        for (HrSalaryStructure s : rows) {
            s.setDeptName(deptNameByEmp.get(s.getEmployeeId()));
        }
    }
}
