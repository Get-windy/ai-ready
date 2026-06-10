package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetPurchaseDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetPurchase;
import cn.aiedge.erp.fixedasset.repository.FixedAssetPurchaseRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetPurchaseService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固定资产购置申请服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetPurchaseServiceImpl implements FixedAssetPurchaseService {

    private final FixedAssetPurchaseRepository purchaseRepository;
    private final FixedAssetRepository fixedAssetRepository;

    @Override
    @Transactional
    public FixedAssetPurchaseDTO create(FixedAssetPurchaseDTO dto) {
        FixedAssetPurchase entity = toEntity(dto);
        if (entity.getPurchaseNo() == null || entity.getPurchaseNo().isBlank()) {
            entity.setPurchaseNo("FP" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        }
        entity.setStatus("draft");
        entity.setApplyDate(entity.getApplyDate() != null ? entity.getApplyDate() : LocalDate.now());
        entity.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetPurchaseDTO update(Long id, FixedAssetPurchaseDTO dto) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        updateEntity(entity, dto);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        entity.markAsDeleted();
        purchaseRepository.save(entity);
    }

    @Override
    public FixedAssetPurchaseDTO getById(Long id) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public Page<FixedAssetPurchaseDTO> getPage(String purchaseNo, String status, String title, Pageable pageable) {
        Specification<FixedAssetPurchase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (StringUtils.hasText(purchaseNo)) {
                predicates.add(cb.like(root.get("purchaseNo"), "%" + purchaseNo + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(title)) {
                predicates.add(cb.like(root.get("title"), "%" + title + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAssetPurchase> page = purchaseRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public FixedAssetPurchaseDTO submit(Long id) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        if (!"draft".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的申请可以提交");
        }
        entity.setStatus("pending");
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetPurchaseDTO approve(Long id, String comment) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        if (!"pending".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有待审批状态的申请可以通过");
        }
        entity.setStatus("approved");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetPurchaseDTO reject(Long id, String comment) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        if (!"pending".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有待审批状态的申请可以拒绝");
        }
        entity.setStatus("rejected");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetPurchaseDTO accept(Long id, FixedAssetPurchaseDTO dto) {
        FixedAssetPurchase entity = purchaseRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("购置申请不存在: " + id));
        if (!"approved".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有已审批通过的申请可以入库验收");
        }

        // Update purchase entity with actual delivery info
        if (dto.getActualAmount() != null) entity.setActualAmount(dto.getActualAmount());
        if (dto.getActualDeliveryDate() != null) entity.setActualDeliveryDate(dto.getActualDeliveryDate());
        if (dto.getSupplierName() != null) entity.setSupplierName(dto.getSupplierName());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());

        // Create asset card
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode("FA" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        asset.setAssetName(entity.getAssetName());
        asset.setCategoryId(entity.getCategoryId());
        asset.setCategoryName(entity.getCategoryName());
        asset.setOriginalValue(dto.getActualAmount() != null ? dto.getActualAmount() : entity.getEstimatedAmount());
        asset.setNetValue(dto.getActualAmount() != null ? dto.getActualAmount() : entity.getEstimatedAmount());
        asset.setPurchaseDate(dto.getActualDeliveryDate() != null ? dto.getActualDeliveryDate() : LocalDate.now());
        asset.setSpecification(entity.getSpecification());
        asset.setSupplierName(dto.getSupplierName() != null ? dto.getSupplierName() : entity.getSupplierName());
        asset.setDepartmentId(entity.getDepartmentId());
        asset.setDepartmentName(entity.getDepartmentName());
        asset.setStatus("in_use");
        asset.setUseStatus("in_use");
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        asset = fixedAssetRepository.save(asset);

        // Link generated asset to purchase record
        entity.setGeneratedAssetId(asset.getId());
        entity.setStatus("accepted");
        entity.setActualDeliveryDate(dto.getActualDeliveryDate() != null ? dto.getActualDeliveryDate() : LocalDate.now());
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = purchaseRepository.save(entity);

        return toDTO(entity);
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", purchaseRepository.countNotDeleted());
        stats.put("draftCount", purchaseRepository.countByStatus("draft"));
        stats.put("pendingCount", purchaseRepository.countByStatus("pending"));
        stats.put("approvedCount", purchaseRepository.countByStatus("approved"));
        stats.put("rejectedCount", purchaseRepository.countByStatus("rejected"));
        stats.put("acceptedCount", purchaseRepository.countByStatus("accepted"));
        return stats;
    }

    private FixedAssetPurchase toEntity(FixedAssetPurchaseDTO dto) {
        FixedAssetPurchase entity = new FixedAssetPurchase();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAssetPurchase entity, FixedAssetPurchaseDTO dto) {
        entity.setPurchaseNo(dto.getPurchaseNo());
        entity.setTitle(dto.getTitle());
        entity.setApplicantId(dto.getApplicantId());
        entity.setApplicantName(dto.getApplicantName());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setAssetName(dto.getAssetName());
        entity.setCategoryId(dto.getCategoryId());
        entity.setCategoryName(dto.getCategoryName());
        entity.setQuantity(dto.getQuantity());
        entity.setEstimatedAmount(dto.getEstimatedAmount());
        entity.setActualAmount(dto.getActualAmount());
        entity.setApplyDate(dto.getApplyDate());
        entity.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        entity.setActualDeliveryDate(dto.getActualDeliveryDate());
        entity.setPurchaseReason(dto.getPurchaseReason());
        entity.setSpecification(dto.getSpecification());
        entity.setSupplierName(dto.getSupplierName());
        entity.setRemark(dto.getRemark());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setApprovalComment(dto.getApprovalComment());
    }

    private FixedAssetPurchaseDTO toDTO(FixedAssetPurchase entity) {
        FixedAssetPurchaseDTO dto = new FixedAssetPurchaseDTO();
        dto.setId(entity.getId());
        dto.setPurchaseNo(entity.getPurchaseNo());
        dto.setTitle(entity.getTitle());
        dto.setApplicantId(entity.getApplicantId());
        dto.setApplicantName(entity.getApplicantName());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(entity.getDepartmentName());
        dto.setAssetName(entity.getAssetName());
        dto.setCategoryId(entity.getCategoryId());
        dto.setCategoryName(entity.getCategoryName());
        dto.setQuantity(entity.getQuantity());
        dto.setEstimatedAmount(entity.getEstimatedAmount());
        dto.setActualAmount(entity.getActualAmount());
        dto.setApplyDate(entity.getApplyDate());
        dto.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        dto.setActualDeliveryDate(entity.getActualDeliveryDate());
        dto.setPurchaseReason(entity.getPurchaseReason());
        dto.setStatus(entity.getStatus());
        dto.setApprovalComment(entity.getApprovalComment());
        dto.setGeneratedAssetId(entity.getGeneratedAssetId());
        dto.setSpecification(entity.getSpecification());
        dto.setSupplierName(entity.getSupplierName());
        dto.setRemark(entity.getRemark());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }
}
