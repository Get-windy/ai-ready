package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetDepreciation;
import cn.aiedge.erp.fixedasset.repository.FixedAssetDepreciationRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetService;
import cn.aiedge.erp.fixedasset.service.depreciation.DepreciationContext;
import cn.hutool.core.util.IdUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固定资产服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetServiceImpl implements FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final FixedAssetDepreciationRepository depreciationRepository;
    private final DepreciationContext depreciationContext;

    @Override
    @Transactional
    public FixedAssetDTO create(FixedAssetDTO dto) {
        FixedAsset asset = toEntity(dto);
        if (asset.getAssetCode() == null || asset.getAssetCode().isBlank()) {
            asset.setAssetCode("FA" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        }
        asset.setStatus("draft");
        if (asset.getOriginalValue() != null) {
            asset.setNetValue(asset.getOriginalValue());
        }
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        asset = fixedAssetRepository.save(asset);
        return toDTO(asset);
    }

    @Override
    @Transactional
    public FixedAssetDTO update(Long id, FixedAssetDTO dto) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("固定资产不存在: " + id));
        updateEntity(asset, dto);
        asset.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        asset = fixedAssetRepository.save(asset);
        return toDTO(asset);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("固定资产不存在: " + id));
        asset.markAsDeleted();
        asset.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        fixedAssetRepository.save(asset);
    }

    @Override
    public FixedAssetDTO getById(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("固定资产不存在: " + id));
        return toDTO(asset);
    }

    @Override
    public Page<FixedAssetDTO> getPage(String assetCode, String assetName, Long categoryId, String status,
                                       String departmentId, String keyword, Pageable pageable) {
        Specification<FixedAsset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (StringUtils.hasText(assetCode)) {
                predicates.add(cb.like(root.get("assetCode"), "%" + assetCode + "%"));
            }
            if (StringUtils.hasText(assetName)) {
                predicates.add(cb.like(root.get("assetName"), "%" + assetName + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(departmentId)) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                    cb.like(root.get("assetCode"), pattern),
                    cb.like(root.get("assetName"), pattern),
                    cb.like(root.get("brand"), pattern),
                    cb.like(root.get("specification"), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAsset> page = fixedAssetRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public FixedAssetDTO depreciate(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("固定资产不存在: " + id));

        if (!"active".equals(asset.getStatus())) {
            throw BusinessException.badRequest("只有已启用的资产才能计提折旧");
        }

        Map<String, BigDecimal> result = depreciationContext.calculateDepreciation(asset);

        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        FixedAssetDepreciation depreciation = new FixedAssetDepreciation();
        depreciation.setAssetId(asset.getId());
        depreciation.setPeriod(period);
        depreciation.setDepreciationDate(LocalDate.now());
        depreciation.setPeriodAmount(result.get("periodAmount"));
        depreciation.setAccumulatedDepreciation(result.get("accumulatedDepreciation"));
        depreciation.setNetValue(result.get("netValue"));
        depreciation.setAssetOriginalValue(asset.getOriginalValue());
        depreciation.setAssetName(asset.getAssetName());
        depreciation.setAssetCode(asset.getAssetCode());
        depreciation.setStatus("completed");
        depreciationRepository.save(depreciation);

        asset.setAccumulatedDepreciation(result.get("accumulatedDepreciation"));
        asset.setNetValue(result.get("netValue"));
        asset.setMonthlyDepreciation(result.get("periodAmount"));
        asset.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        asset = fixedAssetRepository.save(asset);

        return toDTO(asset);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    public List<FixedAssetDTO> exportList(String assetCode, String assetName, Long categoryId, String status,
                                           String departmentId, String keyword) {
        Page<FixedAssetDTO> page = getPage(assetCode, assetName, categoryId, status, departmentId, keyword,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt")));
        return page.getContent();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", fixedAssetRepository.countNotDeleted());
        stats.put("activeCount", fixedAssetRepository.countByStatusAndNotDeleted("active"));
        stats.put("draftCount", fixedAssetRepository.countByStatusAndNotDeleted("draft"));
        stats.put("disposedCount", fixedAssetRepository.countByStatusAndNotDeleted("disposed"));
        stats.put("totalOriginalValue", fixedAssetRepository.sumOriginalValue());
        stats.put("totalNetValue", fixedAssetRepository.sumNetValue());
        stats.put("totalAccumulatedDepreciation", fixedAssetRepository.sumAccumulatedDepreciation());
        return stats;
    }

    private FixedAsset toEntity(FixedAssetDTO dto) {
        FixedAsset entity = new FixedAsset();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAsset entity, FixedAssetDTO dto) {
        entity.setAssetCode(dto.getAssetCode());
        entity.setAssetName(dto.getAssetName());
        entity.setCategoryId(dto.getCategoryId());
        entity.setCategoryName(dto.getCategoryName());
        entity.setPurchaseDate(dto.getPurchaseDate());
        entity.setOriginalValue(dto.getOriginalValue());
        entity.setNetValue(dto.getNetValue());
        entity.setDepreciationMethod(dto.getDepreciationMethod());
        entity.setUsefulLife(dto.getUsefulLife());
        entity.setSalvageValue(dto.getSalvageValue());
        entity.setSalvageRate(dto.getSalvageRate());
        entity.setMonthlyDepreciation(dto.getMonthlyDepreciation());
        entity.setAccumulatedDepreciation(dto.getAccumulatedDepreciation());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setLocation(dto.getLocation());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setCustodianId(dto.getCustodianId());
        entity.setCustodianName(dto.getCustodianName());
        entity.setSpecification(dto.getSpecification());
        entity.setBrand(dto.getBrand());
        entity.setSupplierName(dto.getSupplierName());
        entity.setInvoiceNo(dto.getInvoiceNo());
        entity.setWarrantyEndDate(dto.getWarrantyEndDate());
        entity.setDescription(dto.getDescription());
        entity.setUseStatus(dto.getUseStatus());
        entity.setAssetPhoto(dto.getAssetPhoto());
        entity.setRemark(dto.getRemark());
    }

    private FixedAssetDTO toDTO(FixedAsset entity) {
        FixedAssetDTO dto = new FixedAssetDTO();
        dto.setId(entity.getId());
        dto.setAssetCode(entity.getAssetCode());
        dto.setAssetName(entity.getAssetName());
        dto.setCategoryId(entity.getCategoryId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setPurchaseDate(entity.getPurchaseDate());
        dto.setOriginalValue(entity.getOriginalValue());
        dto.setNetValue(entity.getNetValue());
        dto.setDepreciationMethod(entity.getDepreciationMethod());
        dto.setUsefulLife(entity.getUsefulLife());
        dto.setSalvageValue(entity.getSalvageValue());
        dto.setSalvageRate(entity.getSalvageRate());
        dto.setMonthlyDepreciation(entity.getMonthlyDepreciation());
        dto.setAccumulatedDepreciation(entity.getAccumulatedDepreciation());
        dto.setStatus(entity.getStatus());
        dto.setLocation(entity.getLocation());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(entity.getDepartmentName());
        dto.setCustodianId(entity.getCustodianId());
        dto.setCustodianName(entity.getCustodianName());
        dto.setSpecification(entity.getSpecification());
        dto.setBrand(entity.getBrand());
        dto.setSupplierName(entity.getSupplierName());
        dto.setInvoiceNo(entity.getInvoiceNo());
        dto.setWarrantyEndDate(entity.getWarrantyEndDate());
        dto.setDescription(entity.getDescription());
        dto.setUseStatus(entity.getUseStatus());
        dto.setAssetPhoto(entity.getAssetPhoto());
        dto.setRemark(entity.getRemark());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }
}
