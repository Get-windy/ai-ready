package cn.aiedge.hr.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.hr.employee.HrContract;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrContractMapper;
import cn.aiedge.hr.service.HrContractService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 劳动合同服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrContractServiceImpl extends ServiceImpl<HrContractMapper, HrContract>
        implements HrContractService {

    /** 合同状态：待签 / 生效 / 到期 / 终止 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_EXPIRED = 2;
    private static final int STATUS_TERMINATED = 3;

    private final HrLookupHelper lookupHelper;
    private final BizNumberGeneratorService numberGenerator;

    @Override
    public Page<HrContract> pageContracts(Page<HrContract> page, Long tenantId,
                                          Long employeeId, String contractNo,
                                          Integer contractType, Integer status) {
        Page<HrContract> result = page(page, new LambdaQueryWrapper<HrContract>()
                .eq(tenantId != null, HrContract::getTenantId, tenantId)
                .eq(employeeId != null, HrContract::getEmployeeId, employeeId)
                .like(StringUtils.hasText(contractNo), HrContract::getContractNo, contractNo)
                .eq(contractType != null, HrContract::getContractType, contractType)
                .eq(status != null, HrContract::getStatus, status)
                .orderByDesc(HrContract::getCreateTime)
                .orderByDesc(HrContract::getId));
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrContract> getByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        List<HrContract> rows = list(new LambdaQueryWrapper<HrContract>()
                .eq(HrContract::getEmployeeId, employeeId)
                .orderByDesc(HrContract::getStartDate));
        enrich(rows);
        return rows;
    }

    @Override
    public String nextContractNo() {
        return numberGenerator.nextNumber("HT", SecurityUtils.getCurrentTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(HrContract contract) {
        if (contract.getEmployeeId() == null) {
            throw new BusinessException("请选择员工");
        }
        validateDates(contract);
        if (!StringUtils.hasText(contract.getContractNo())) {
            contract.setContractNo(nextContractNo());
        }
        if (contract.getStatus() == null) {
            contract.setStatus(STATUS_ACTIVE);
        }
        contract.setTenantId(SecurityUtils.getCurrentTenantId());
        contract.setCreateTime(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        save(contract);
        log.info("创建劳动合同: employeeId={}, contractNo={}", contract.getEmployeeId(), contract.getContractNo());
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(HrContract contract) {
        if (contract.getId() == null) {
            throw new BusinessException("合同ID不能为空");
        }
        HrContract exists = getById(contract.getId());
        if (exists == null) {
            throw new BusinessException("合同不存在");
        }
        validateDates(contract);
        // 状态只允许经 updateContractStatus 变更，避免表单直改状态绕过状态机
        contract.setStatus(null);
        contract.setUpdateTime(LocalDateTime.now());
        updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractStatus(Long id, Integer status) {
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        HrContract exists = id == null ? null : getById(id);
        if (exists == null) {
            throw new BusinessException("合同不存在");
        }
        if (Objects.equals(exists.getStatus(), status)) {
            return;
        }
        if (exists.getStatus() != null && exists.getStatus() == STATUS_TERMINATED) {
            throw new BusinessException("已终止的合同不能再次变更状态");
        }
        HrContract update = new HrContract();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
        log.info("合同状态变更: id={}, {} → {}", id, exists.getStatus(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        if (id == null || getById(id) == null) {
            throw new BusinessException("合同不存在");
        }
        removeById(id);
    }

    @Override
    public List<HrContract> listExpiring(int days) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(Math.max(days, 0));
        List<HrContract> rows = list(new LambdaQueryWrapper<HrContract>()
                .eq(HrContract::getStatus, STATUS_ACTIVE)
                .isNotNull(HrContract::getEndDate)
                .ge(HrContract::getEndDate, today)
                .le(HrContract::getEndDate, deadline)
                .orderByAsc(HrContract::getEndDate));
        enrich(rows);
        return rows;
    }

    private void validateDates(HrContract contract) {
        if (contract.getStartDate() != null && contract.getEndDate() != null
                && contract.getStartDate().isAfter(contract.getEndDate())) {
            throw new BusinessException("合同开始日期不能晚于结束日期");
        }
        if (contract.getTrialDateEnd() != null && contract.getStartDate() != null
                && contract.getTrialDateEnd().isBefore(contract.getStartDate())) {
            throw new BusinessException("试用期到期日不能早于合同开始日期");
        }
    }

    private void enrich(List<HrContract> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, HrEmployee> empMap = lookupHelper.employees(rows.stream()
                .map(HrContract::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        for (HrContract c : rows) {
            HrEmployee emp = empMap.get(c.getEmployeeId());
            if (emp != null) {
                c.setEmployeeName(emp.getEmployeeName());
                c.setEmployeeNo(emp.getEmployeeNo());
            }
        }
    }
}
