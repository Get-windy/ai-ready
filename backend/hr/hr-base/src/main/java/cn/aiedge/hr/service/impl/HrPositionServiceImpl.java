package cn.aiedge.hr.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrEmployeeMapper;
import cn.aiedge.hr.mapper.HrPositionMapper;
import cn.aiedge.hr.organization.HrPosition;
import cn.aiedge.hr.service.HrPositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HR 岗位（职位）服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrPositionServiceImpl extends ServiceImpl<HrPositionMapper, HrPosition>
        implements HrPositionService {

    private final HrEmployeeMapper employeeMapper;
    private final HrLookupHelper lookupHelper;
    private final BizNumberGeneratorService numberGenerator;

    @Override
    public Page<HrPosition> pagePositions(Page<HrPosition> page, Long tenantId,
                                          Long deptId, String positionName,
                                          String positionCode, Integer positionLevel, Integer status) {
        LambdaQueryWrapper<HrPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, HrPosition::getTenantId, tenantId)
                .eq(deptId != null, HrPosition::getDeptId, deptId)
                .like(positionName != null && !positionName.isEmpty(), HrPosition::getPositionName, positionName)
                .like(positionCode != null && !positionCode.isEmpty(), HrPosition::getPositionCode, positionCode)
                .eq(positionLevel != null, HrPosition::getPositionLevel, positionLevel)
                .eq(status != null, HrPosition::getStatus, status)
                .orderByAsc(HrPosition::getSort)
                .orderByAsc(HrPosition::getId);
        Page<HrPosition> result = page(page, wrapper);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<HrPosition> getByDeptId(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }
        List<HrPosition> rows = list(new LambdaQueryWrapper<HrPosition>()
                .eq(HrPosition::getDeptId, deptId)
                .orderByAsc(HrPosition::getSort));
        enrich(rows);
        return rows;
    }

    @Override
    public List<HrPosition> listEnabled() {
        List<HrPosition> rows = list(new LambdaQueryWrapper<HrPosition>()
                .eq(HrPosition::getStatus, 1)
                .orderByAsc(HrPosition::getSort));
        enrich(rows);
        return rows;
    }

    @Override
    public String nextPositionCode() {
        return numberGenerator.nextNumber("HRPOS", cn.aiedge.base.utils.SecurityUtils.getCurrentTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPosition(HrPosition position) {
        if (position.getPositionName() == null || position.getPositionName().trim().isEmpty()) {
            throw new BusinessException("岗位名称不能为空");
        }
        if (position.getPositionCode() == null || position.getPositionCode().trim().isEmpty()) {
            position.setPositionCode(nextPositionCode());
        } else if (existsCode(position.getPositionCode(), null)) {
            throw new BusinessException("岗位编码已存在：" + position.getPositionCode());
        }
        if (position.getStatus() == null) {
            position.setStatus(1);
        }
        if (position.getQuotaCount() == null) {
            position.setQuotaCount(0);
        }
        if (position.getCurrentCount() == null) {
            position.setCurrentCount(0);
        }
        if (position.getSort() == null) {
            position.setSort(0);
        }
        position.setCreateTime(LocalDateTime.now());
        position.setUpdateTime(LocalDateTime.now());
        save(position);
        log.info("创建岗位: code={}, name={}", position.getPositionCode(), position.getPositionName());
        return position.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePosition(HrPosition position) {
        if (position.getId() == null) {
            throw new BusinessException("岗位ID不能为空");
        }
        HrPosition exists = getById(position.getId());
        if (exists == null) {
            throw new BusinessException("岗位不存在");
        }
        if (position.getPositionCode() != null && !position.getPositionCode().trim().isEmpty()
                && existsCode(position.getPositionCode(), position.getId())) {
            throw new BusinessException("岗位编码已存在：" + position.getPositionCode());
        }
        // current_count 是派生字段，不接受表单直改
        position.setCurrentCount(null);
        position.setUpdateTime(LocalDateTime.now());
        updateById(position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        Long inPosition = employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getPositionId, id)
                .in(HrEmployee::getStatus, 1, 2));
        if (inPosition != null && inPosition > 0) {
            throw new BusinessException("该岗位下仍有 " + inPosition + " 名在岗/试用员工，不能删除");
        }
        removeById(id);
        log.info("删除岗位: id={}", id);
    }

    @Override
    public void refreshCurrentCount(Long positionId) {
        if (positionId == null) {
            return;
        }
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployee>()
                .eq(HrEmployee::getPositionId, positionId)
                .in(HrEmployee::getStatus, 1, 2));
        HrPosition update = new HrPosition();
        update.setId(positionId);
        update.setCurrentCount(count == null ? 0 : count.intValue());
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
    }

    @Override
    public Map<String, Object> statistics(Long deptId) {
        List<HrPosition> rows = list(new LambdaQueryWrapper<HrPosition>()
                .eq(deptId != null, HrPosition::getDeptId, deptId)
                .eq(HrPosition::getStatus, 1));
        long quota = rows.stream().mapToLong(p -> p.getQuotaCount() == null ? 0 : p.getQuotaCount()).sum();
        long current = rows.stream().mapToLong(p -> p.getCurrentCount() == null ? 0 : p.getCurrentCount()).sum();
        long over = rows.stream()
                .filter(p -> p.getQuotaCount() != null && p.getQuotaCount() > 0
                        && p.getCurrentCount() != null && p.getCurrentCount() > p.getQuotaCount())
                .count();
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("positionCount", rows.size());
        stat.put("quotaTotal", quota);
        stat.put("currentTotal", current);
        stat.put("vacancyTotal", Math.max(0, quota - current));
        stat.put("overQuotaCount", over);
        return stat;
    }

    private boolean existsCode(String code, Long excludeId) {
        Long c = baseMapper.selectCount(new LambdaQueryWrapper<HrPosition>()
                .eq(HrPosition::getPositionCode, code)
                .ne(excludeId != null, HrPosition::getId, excludeId));
        return c != null && c > 0;
    }

    /** 回填部门名与超编标记 */
    private void enrich(List<HrPosition> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, String> deptNames = lookupHelper.deptNames(rows.stream()
                .map(HrPosition::getDeptId).filter(java.util.Objects::nonNull).collect(Collectors.toList()));
        for (HrPosition p : rows) {
            p.setDeptName(p.getDeptId() == null ? null : deptNames.get(p.getDeptId()));
            p.setOverQuota(p.getQuotaCount() != null && p.getQuotaCount() > 0
                    && p.getCurrentCount() != null && p.getCurrentCount() > p.getQuotaCount());
        }
    }
}
