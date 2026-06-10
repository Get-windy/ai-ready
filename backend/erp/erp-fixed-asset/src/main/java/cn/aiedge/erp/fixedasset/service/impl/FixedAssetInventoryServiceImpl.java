package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetInventoryDTO;
import cn.aiedge.erp.fixedasset.model.FixedAssetInventory;
import cn.aiedge.erp.fixedasset.repository.FixedAssetInventoryRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetInventoryService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定资产盘点服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetInventoryServiceImpl implements FixedAssetInventoryService {

    private final FixedAssetInventoryRepository inventoryRepository;

    @Override
    @Transactional
    public FixedAssetInventoryDTO create(FixedAssetInventoryDTO dto) {
        FixedAssetInventory entity = toEntity(dto);
        if (entity.getInventoryNo() == null || entity.getInventoryNo().isBlank()) {
            entity.setInventoryNo("FI" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        }
        entity.setStatus("pending");
        entity.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = inventoryRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetInventoryDTO update(Long id, FixedAssetInventoryDTO dto) {
        FixedAssetInventory entity = inventoryRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("盘点记录不存在: " + id));
        updateEntity(entity, dto);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = inventoryRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    public FixedAssetInventoryDTO getById(Long id) {
        FixedAssetInventory entity = inventoryRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("盘点记录不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public Page<FixedAssetInventoryDTO> getPage(String inventoryNo, String status, String checkResult, Pageable pageable) {
        Specification<FixedAssetInventory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (StringUtils.hasText(inventoryNo)) {
                predicates.add(cb.like(root.get("inventoryNo"), "%" + inventoryNo + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(checkResult)) {
                predicates.add(cb.equal(root.get("checkResult"), checkResult));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAssetInventory> page = inventoryRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    private FixedAssetInventory toEntity(FixedAssetInventoryDTO dto) {
        FixedAssetInventory entity = new FixedAssetInventory();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAssetInventory entity, FixedAssetInventoryDTO dto) {
        entity.setInventoryNo(dto.getInventoryNo());
        entity.setInventoryDate(dto.getInventoryDate());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setAssetId(dto.getAssetId());
        entity.setAssetCode(dto.getAssetCode());
        entity.setAssetName(dto.getAssetName());
        entity.setExpectedLocation(dto.getExpectedLocation());
        entity.setActualLocation(dto.getActualLocation());
        entity.setExpectedStatus(dto.getExpectedStatus());
        entity.setActualStatus(dto.getActualStatus());
        entity.setExpectedCustodian(dto.getExpectedCustodian());
        entity.setActualCustodian(dto.getActualCustodian());
        entity.setCheckResult(dto.getCheckResult());
        entity.setRemark(dto.getRemark());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
    }

    private FixedAssetInventoryDTO toDTO(FixedAssetInventory entity) {
        FixedAssetInventoryDTO dto = new FixedAssetInventoryDTO();
        dto.setId(entity.getId());
        dto.setInventoryNo(entity.getInventoryNo());
        dto.setInventoryDate(entity.getInventoryDate());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(entity.getDepartmentName());
        dto.setAssetId(entity.getAssetId());
        dto.setAssetCode(entity.getAssetCode());
        dto.setAssetName(entity.getAssetName());
        dto.setExpectedLocation(entity.getExpectedLocation());
        dto.setActualLocation(entity.getActualLocation());
        dto.setExpectedStatus(entity.getExpectedStatus());
        dto.setActualStatus(entity.getActualStatus());
        dto.setExpectedCustodian(entity.getExpectedCustodian());
        dto.setActualCustodian(entity.getActualCustodian());
        dto.setCheckResult(entity.getCheckResult());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
