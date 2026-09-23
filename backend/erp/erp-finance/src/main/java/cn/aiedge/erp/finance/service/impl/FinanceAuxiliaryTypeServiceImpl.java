package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryTypeMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryType;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 辅助核算类型Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAuxiliaryTypeServiceImpl implements FinanceAuxiliaryTypeService {

    /** 无租户上下文时回落的租户（定时任务/初始化场景，与既有初始化数据一致） */
    private static final Long FALLBACK_TENANT_ID = 1L;

    /**
     * 本模块所属租户：会话上下文优先，取不到才回落。
     *
     * <p><b>2026-09-23 修复</b>：此前注释自称「本模块暂按单租户处理」而写死租户 1 ⇒
     * 非 1 租户查不到自己的辅助核算类型、新建的也落到租户 1 名下（跨租户错写）。
     * 取法与 {@code AccountingPeriodServiceImpl#currentTenantId} 保持一致。</p>
     */
    private Long currentTenantId() {
        Long tid = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        return tid != null ? tid : FALLBACK_TENANT_ID;
    }

    private final FinanceAuxiliaryTypeMapper financeAuxiliaryTypeMapper;

    @Override
    public IPage<FinanceAuxiliaryTypeDTO> page(String typeCode, String typeName, Boolean enabled, Page<FinanceAuxiliaryTypeDTO> page) {
        LambdaQueryWrapper<FinanceAuxiliaryType> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryType>()
                .eq(FinanceAuxiliaryType::getTenantId, currentTenantId())
                .like(typeCode != null && !typeCode.isBlank(), FinanceAuxiliaryType::getTypeCode, typeCode)
                .like(typeName != null && !typeName.isBlank(), FinanceAuxiliaryType::getTypeName, typeName)
                .eq(enabled != null, FinanceAuxiliaryType::getEnabled, enabled)
                .orderByAsc(FinanceAuxiliaryType::getSort)
                .orderByAsc(FinanceAuxiliaryType::getId);
        Page<FinanceAuxiliaryType> entityPage = financeAuxiliaryTypeMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return entityPage.convert(this::toDTO);
    }

    @Override
    public List<FinanceAuxiliaryTypeDTO> list(Boolean enabled) {
        LambdaQueryWrapper<FinanceAuxiliaryType> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryType>()
                .eq(FinanceAuxiliaryType::getTenantId, currentTenantId())
                .eq(enabled != null, FinanceAuxiliaryType::getEnabled, enabled)
                .orderByAsc(FinanceAuxiliaryType::getSort)
                .orderByAsc(FinanceAuxiliaryType::getId);
        return financeAuxiliaryTypeMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FinanceAuxiliaryTypeDTO getById(Long id) {
        FinanceAuxiliaryType entity = financeAuxiliaryTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算类型不存在: " + id);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FinanceAuxiliaryTypeDTO create(FinanceAuxiliaryTypeDTO dto) {
        financeAuxiliaryTypeMapper.findByTypeCode(dto.getTypeCode(), currentTenantId())
                .ifPresent(t -> {
                    throw BusinessException.badRequest("辅助核算类型编码已存在: " + dto.getTypeCode());
                });

        FinanceAuxiliaryType entity = new FinanceAuxiliaryType();
        entity.setTenantId(currentTenantId());
        entity.setTypeCode(dto.getTypeCode());
        entity.setTypeName(dto.getTypeName());
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setRemark(dto.getRemark());
        financeAuxiliaryTypeMapper.insert(entity);
        log.info("创建辅助核算类型: id={}, code={}, name={}", entity.getId(), entity.getTypeCode(), entity.getTypeName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FinanceAuxiliaryTypeDTO update(Long id, FinanceAuxiliaryTypeDTO dto) {
        FinanceAuxiliaryType entity = financeAuxiliaryTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算类型不存在: " + id);
        }

        // 如果修改了编码，检查新编码是否已存在
        if (dto.getTypeCode() != null && !dto.getTypeCode().equals(entity.getTypeCode())) {
            financeAuxiliaryTypeMapper.findByTypeCode(dto.getTypeCode(), currentTenantId())
                    .ifPresent(t -> {
                        throw BusinessException.badRequest("辅助核算类型编码已存在: " + dto.getTypeCode());
                    });
            entity.setTypeCode(dto.getTypeCode());
        }

        if (dto.getTypeName() != null) {
            entity.setTypeName(dto.getTypeName());
        }
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        if (dto.getSort() != null) {
            entity.setSort(dto.getSort());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
        financeAuxiliaryTypeMapper.updateById(entity);
        log.info("更新辅助核算类型: id={}, code={}, name={}", id, entity.getTypeCode(), entity.getTypeName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FinanceAuxiliaryTypeDTO enable(Long id, Boolean enabled) {
        FinanceAuxiliaryType entity = financeAuxiliaryTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算类型不存在: " + id);
        }
        // 前端开关一定传值；缺省时按「启用」处理，与 create 的默认口径保持一致
        entity.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        financeAuxiliaryTypeMapper.updateById(entity);
        log.info("启用/禁用辅助核算类型: id={}, code={}, enabled={}", id, entity.getTypeCode(), entity.getEnabled());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FinanceAuxiliaryType entity = financeAuxiliaryTypeMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算类型不存在: " + id);
        }
        financeAuxiliaryTypeMapper.deleteById(id);
        log.info("删除辅助核算类型: id={}, code={}", id, entity.getTypeCode());
    }

    private FinanceAuxiliaryTypeDTO toDTO(FinanceAuxiliaryType entity) {
        FinanceAuxiliaryTypeDTO dto = new FinanceAuxiliaryTypeDTO();
        dto.setId(entity.getId());
        dto.setTypeCode(entity.getTypeCode());
        dto.setTypeName(entity.getTypeName());
        dto.setEnabled(entity.getEnabled());
        dto.setSort(entity.getSort());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
