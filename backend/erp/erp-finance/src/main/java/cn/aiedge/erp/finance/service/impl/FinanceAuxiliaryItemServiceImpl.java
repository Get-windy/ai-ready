package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryItemDTO;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryItemMapper;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryTypeMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryItem;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryType;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryItemService;
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
 * 辅助核算项目Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAuxiliaryItemServiceImpl implements FinanceAuxiliaryItemService {

    /**
     * 默认租户ID（本模块暂按单租户处理）
     */
    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FinanceAuxiliaryItemMapper financeAuxiliaryItemMapper;
    private final FinanceAuxiliaryTypeMapper financeAuxiliaryTypeMapper;

    @Override
    public IPage<FinanceAuxiliaryItemDTO> page(Long auxiliaryTypeId, String itemCode, String itemName, Boolean enabled, Page<FinanceAuxiliaryItemDTO> page) {
        LambdaQueryWrapper<FinanceAuxiliaryItem> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryItem>()
                .eq(FinanceAuxiliaryItem::getTenantId, DEFAULT_TENANT_ID)
                .eq(auxiliaryTypeId != null, FinanceAuxiliaryItem::getAuxiliaryTypeId, auxiliaryTypeId)
                .like(itemCode != null && !itemCode.isBlank(), FinanceAuxiliaryItem::getItemCode, itemCode)
                .like(itemName != null && !itemName.isBlank(), FinanceAuxiliaryItem::getItemName, itemName)
                .eq(enabled != null, FinanceAuxiliaryItem::getEnabled, enabled)
                .orderByAsc(FinanceAuxiliaryItem::getSort)
                .orderByAsc(FinanceAuxiliaryItem::getId);
        Page<FinanceAuxiliaryItem> entityPage = financeAuxiliaryItemMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return entityPage.convert(this::toDTO);
    }

    @Override
    public List<FinanceAuxiliaryItemDTO> listByTypeId(Long auxiliaryTypeId, Boolean enabled) {
        LambdaQueryWrapper<FinanceAuxiliaryItem> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryItem>()
                .eq(FinanceAuxiliaryItem::getTenantId, DEFAULT_TENANT_ID)
                .eq(auxiliaryTypeId != null, FinanceAuxiliaryItem::getAuxiliaryTypeId, auxiliaryTypeId)
                .eq(enabled != null, FinanceAuxiliaryItem::getEnabled, enabled)
                .orderByAsc(FinanceAuxiliaryItem::getSort)
                .orderByAsc(FinanceAuxiliaryItem::getId);
        return financeAuxiliaryItemMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public FinanceAuxiliaryItemDTO getById(Long id) {
        FinanceAuxiliaryItem entity = financeAuxiliaryItemMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算项目不存在: " + id);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FinanceAuxiliaryItemDTO create(FinanceAuxiliaryItemDTO dto) {
        // 验证辅助核算类型是否存在
        FinanceAuxiliaryType type = financeAuxiliaryTypeMapper.selectById(dto.getAuxiliaryTypeId());
        if (type == null) {
            throw BusinessException.notFound("辅助核算类型不存在: " + dto.getAuxiliaryTypeId());
        }

        // 检查同一类型下编码是否重复
        financeAuxiliaryItemMapper.findByItemCode(dto.getItemCode(), dto.getAuxiliaryTypeId(), DEFAULT_TENANT_ID)
                .ifPresent(i -> {
                    throw BusinessException.badRequest("辅助核算项目编码已存在: " + dto.getItemCode());
                });

        // 如果有上级ID，验证上级是否存在（parent_id = 0 是本表「顶级」口径，不参与校验）
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            FinanceAuxiliaryItem parent = financeAuxiliaryItemMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw BusinessException.notFound("上级辅助核算项目不存在: " + dto.getParentId());
            }
        }

        FinanceAuxiliaryItem entity = new FinanceAuxiliaryItem();
        entity.setTenantId(DEFAULT_TENANT_ID);
        entity.setAuxiliaryTypeId(dto.getAuxiliaryTypeId());
        entity.setItemCode(dto.getItemCode());
        entity.setItemName(dto.getItemName());
        entity.setParentId(dto.getParentId());
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setRemark(dto.getRemark());
        financeAuxiliaryItemMapper.insert(entity);
        log.info("创建辅助核算项目: id={}, typeCode={}, itemCode={}, itemName={}",
                entity.getId(), type.getTypeCode(), entity.getItemCode(), entity.getItemName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FinanceAuxiliaryItemDTO update(Long id, FinanceAuxiliaryItemDTO dto) {
        FinanceAuxiliaryItem entity = financeAuxiliaryItemMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算项目不存在: " + id);
        }

        // 如果修改了类型ID，验证新类型是否存在
        if (dto.getAuxiliaryTypeId() != null && !dto.getAuxiliaryTypeId().equals(entity.getAuxiliaryTypeId())) {
            FinanceAuxiliaryType type = financeAuxiliaryTypeMapper.selectById(dto.getAuxiliaryTypeId());
            if (type == null) {
                throw BusinessException.notFound("辅助核算类型不存在: " + dto.getAuxiliaryTypeId());
            }
            entity.setAuxiliaryTypeId(dto.getAuxiliaryTypeId());
        }

        // 如果修改了编码，检查新编码是否已存在
        if (dto.getItemCode() != null && !dto.getItemCode().equals(entity.getItemCode())) {
            financeAuxiliaryItemMapper.findByItemCode(dto.getItemCode(), entity.getAuxiliaryTypeId(), DEFAULT_TENANT_ID)
                    .ifPresent(i -> {
                        throw BusinessException.badRequest("辅助核算项目编码已存在: " + dto.getItemCode());
                    });
            entity.setItemCode(dto.getItemCode());
        }

        if (dto.getItemName() != null) {
            entity.setItemName(dto.getItemName());
        }

        // 如果修改了上级ID，验证新上级是否存在（parent_id = 0 表示置为顶级，无需校验）
        if (dto.getParentId() != null && !dto.getParentId().equals(entity.getParentId())) {
            if (dto.getParentId() > 0) {
                FinanceAuxiliaryItem parent = financeAuxiliaryItemMapper.selectById(dto.getParentId());
                if (parent == null) {
                    throw BusinessException.notFound("上级辅助核算项目不存在: " + dto.getParentId());
                }
            }
            entity.setParentId(dto.getParentId());
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

        financeAuxiliaryItemMapper.updateById(entity);
        log.info("更新辅助核算项目: id={}, itemCode={}, itemName={}", id, entity.getItemCode(), entity.getItemName());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FinanceAuxiliaryItem entity = financeAuxiliaryItemMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("辅助核算项目不存在: " + id);
        }

        // 检查是否有下级项目
        LambdaQueryWrapper<FinanceAuxiliaryItem> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryItem>()
                .eq(FinanceAuxiliaryItem::getParentId, id)
                .eq(FinanceAuxiliaryItem::getTenantId, DEFAULT_TENANT_ID);
        Long count = financeAuxiliaryItemMapper.selectCount(wrapper);
        if (count > 0) {
            throw BusinessException.badRequest("该辅助核算项目存在下级项目，无法删除");
        }

        financeAuxiliaryItemMapper.deleteById(id);
        log.info("删除辅助核算项目: id={}, itemCode={}", id, entity.getItemCode());
    }

    private FinanceAuxiliaryItemDTO toDTO(FinanceAuxiliaryItem entity) {
        FinanceAuxiliaryItemDTO dto = new FinanceAuxiliaryItemDTO();
        dto.setId(entity.getId());
        dto.setAuxiliaryTypeId(entity.getAuxiliaryTypeId());
        dto.setItemCode(entity.getItemCode());
        dto.setItemName(entity.getItemName());
        dto.setParentId(entity.getParentId());
        dto.setEnabled(entity.getEnabled());
        dto.setSort(entity.getSort());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
