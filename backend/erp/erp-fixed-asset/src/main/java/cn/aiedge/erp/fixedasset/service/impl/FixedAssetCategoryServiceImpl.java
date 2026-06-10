package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetCategoryDTO;
import cn.aiedge.erp.fixedasset.model.FixedAssetCategory;
import cn.aiedge.erp.fixedasset.repository.FixedAssetCategoryRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 固定资产分类服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetCategoryServiceImpl implements FixedAssetCategoryService {

    private final FixedAssetCategoryRepository categoryRepository;

    @Override
    @Transactional
    public FixedAssetCategoryDTO create(FixedAssetCategoryDTO dto) {
        FixedAssetCategory entity = toEntity(dto);
        entity.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = categoryRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetCategoryDTO update(Long id, FixedAssetCategoryDTO dto) {
        FixedAssetCategory entity = categoryRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("分类不存在: " + id));
        updateEntity(entity, dto);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = categoryRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAssetCategory entity = categoryRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("分类不存在: " + id));
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity.markAsDeleted();
        categoryRepository.save(entity);
    }

    @Override
    public FixedAssetCategoryDTO getById(Long id) {
        FixedAssetCategory entity = categoryRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("分类不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public List<FixedAssetCategoryDTO> getAll() {
        return categoryRepository.findAll().stream()
            .filter(c -> !c.isDeleted())
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTree() {
        List<FixedAssetCategory> all = categoryRepository.findAll().stream()
            .filter(c -> !c.isDeleted())
            .collect(Collectors.toList());
        List<FixedAssetCategory> roots = all.stream()
            .filter(c -> c.getParentId() == null || c.getParentId() == 0)
            .sorted(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0))
            .collect(Collectors.toList());

        return roots.stream()
            .map(root -> buildTreeMap(root, all))
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildTreeMap(FixedAssetCategory node, List<FixedAssetCategory> all) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.getId());
        map.put("key", node.getId().toString());
        map.put("title", node.getCategoryName());
        map.put("categoryCode", node.getCategoryCode());
        map.put("sortOrder", node.getSortOrder());
        map.put("description", node.getDescription());

        List<FixedAssetCategory> children = all.stream()
            .filter(c -> node.getId().equals(c.getParentId()))
            .sorted(Comparator.comparingInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0))
            .collect(Collectors.toList());

        if (!children.isEmpty()) {
            map.put("children", children.stream()
                .map(child -> buildTreeMap(child, all))
                .collect(Collectors.toList()));
        }

        return map;
    }

    private FixedAssetCategory toEntity(FixedAssetCategoryDTO dto) {
        FixedAssetCategory entity = new FixedAssetCategory();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAssetCategory entity, FixedAssetCategoryDTO dto) {
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setCategoryName(dto.getCategoryName());
        entity.setParentId(dto.getParentId());
        entity.setSortOrder(dto.getSortOrder());
        entity.setDefaultDepreciationMethod(dto.getDefaultDepreciationMethod());
        entity.setDefaultUsefulLife(dto.getDefaultUsefulLife());
        entity.setDescription(dto.getDescription());
        entity.setRemark(dto.getRemark());
    }

    private FixedAssetCategoryDTO toDTO(FixedAssetCategory entity) {
        FixedAssetCategoryDTO dto = new FixedAssetCategoryDTO();
        dto.setId(entity.getId());
        dto.setCategoryCode(entity.getCategoryCode());
        dto.setCategoryName(entity.getCategoryName());
        dto.setParentId(entity.getParentId());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDefaultDepreciationMethod(entity.getDefaultDepreciationMethod());
        dto.setDefaultUsefulLife(entity.getDefaultUsefulLife());
        dto.setDescription(entity.getDescription());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
