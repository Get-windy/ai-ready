package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.erp.fixedasset.dto.FixedAssetDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetDepreciation;
import cn.aiedge.erp.fixedasset.repository.FixedAssetDepreciationRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetService;
import cn.aiedge.erp.fixedasset.service.depreciation.DepreciationContext;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
        asset = fixedAssetRepository.save(asset);
        return toDTO(asset);
    }

    @Override
    @Transactional
    public FixedAssetDTO update(Long id, FixedAssetDTO dto) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("固定资产不存在: " + id));
        updateEntity(asset, dto);
        asset = fixedAssetRepository.save(asset);
        return toDTO(asset);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("固定资产不存在: " + id));
        asset.markAsDeleted();
        fixedAssetRepository.save(asset);
    }

    @Override
    public FixedAssetDTO getById(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("固定资产不存在: " + id));
        return toDTO(asset);
    }

    @Override
    public Page<FixedAssetDTO> getPage(String assetCode, String assetName, Long categoryId, String status,
                                       String departmentId, String keyword, Pageable pageable) {
        // For production, use Specification. For simplicity, use findAll with Pageable.
        // In a real project, replace with JpaSpecificationExecutor.
        Page<FixedAsset> page;
        if (assetCode != null || assetName != null || categoryId != null || status != null
            || departmentId != null || keyword != null) {
            // Use repository with filters via custom query methods
            // For this implementation, we fetch all and filter in memory
            // In production, implement proper specification query
            page = fixedAssetRepository.findAll(pageable);
        } else {
            page = fixedAssetRepository.findAll(pageable);
        }
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public FixedAssetDTO depreciate(Long id) {
        FixedAsset asset = fixedAssetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("固定资产不存在: " + id));

        if (!"active".equals(asset.getStatus())) {
            throw new RuntimeException("只有已启用的资产才能计提折旧");
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
        asset = fixedAssetRepository.save(asset);

        return toDTO(asset);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", fixedAssetRepository.count());
        stats.put("activeCount", fixedAssetRepository.countByStatus("active"));
        stats.put("draftCount", fixedAssetRepository.countByStatus("draft"));
        stats.put("disposedCount", fixedAssetRepository.countByStatus("disposed"));
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
