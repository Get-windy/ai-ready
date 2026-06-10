package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDisposalDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetDisposal;
import cn.aiedge.erp.fixedasset.repository.FixedAssetDisposalRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetDisposalService;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 固定资产处置服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetDisposalServiceImpl implements FixedAssetDisposalService {

    private final FixedAssetDisposalRepository disposalRepository;
    private final FixedAssetRepository fixedAssetRepository;

    @Override
    @Transactional
    public FixedAssetDisposalDTO create(FixedAssetDisposalDTO dto) {
        FixedAssetDisposal entity = toEntity(dto);
        if (entity.getDisposalNo() == null || entity.getDisposalNo().isBlank()) {
            entity.setDisposalNo("FD" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        }
        entity.setStatus("draft");
        entity.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));

        // Calculate gain/loss
        if (entity.getDisposalAmount() != null && entity.getNetValue() != null) {
            entity.setGainLoss(entity.getDisposalAmount().subtract(entity.getNetValue()));
        }

        entity = disposalRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetDisposalDTO update(Long id, FixedAssetDisposalDTO dto) {
        FixedAssetDisposal entity = disposalRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("处置记录不存在: " + id));
        updateEntity(entity, dto);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));

        if (entity.getDisposalAmount() != null && entity.getNetValue() != null) {
            entity.setGainLoss(entity.getDisposalAmount().subtract(entity.getNetValue()));
        }

        entity = disposalRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAssetDisposal entity = disposalRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("处置记录不存在: " + id));
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity.markAsDeleted();
        disposalRepository.save(entity);
    }

    @Override
    public FixedAssetDisposalDTO getById(Long id) {
        FixedAssetDisposal entity = disposalRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("处置记录不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public Page<FixedAssetDisposalDTO> getPage(String disposalNo, String status, Pageable pageable) {
        Specification<FixedAssetDisposal> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (StringUtils.hasText(disposalNo)) {
                predicates.add(cb.like(root.get("disposalNo"), "%" + disposalNo + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAssetDisposal> page = disposalRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public FixedAssetDisposalDTO approve(Long id, String comment) {
        FixedAssetDisposal entity = disposalRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("处置记录不存在: " + id));
        entity.setStatus("approved");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = disposalRepository.save(entity);

        // Update asset status
        if (entity.getAssetId() != null) {
            fixedAssetRepository.findById(entity.getAssetId()).ifPresent(asset -> {
                asset.setStatus("disposed");
                asset.setUseStatus("disposed");
                fixedAssetRepository.save(asset);
            });
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetDisposalDTO reject(Long id, String comment) {
        FixedAssetDisposal entity = disposalRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("处置记录不存在: " + id));
        entity.setStatus("rejected");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = disposalRepository.save(entity);
        return toDTO(entity);
    }

    private FixedAssetDisposal toEntity(FixedAssetDisposalDTO dto) {
        FixedAssetDisposal entity = new FixedAssetDisposal();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAssetDisposal entity, FixedAssetDisposalDTO dto) {
        entity.setDisposalNo(dto.getDisposalNo());
        entity.setAssetId(dto.getAssetId());
        entity.setAssetCode(dto.getAssetCode());
        entity.setAssetName(dto.getAssetName());
        entity.setDisposalDate(dto.getDisposalDate());
        entity.setDisposalType(dto.getDisposalType());
        entity.setDisposalAmount(dto.getDisposalAmount());
        entity.setNetValue(dto.getNetValue());
        entity.setGainLoss(dto.getGainLoss());
        entity.setReason(dto.getReason());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setApprovalComment(dto.getApprovalComment());
        entity.setRemark(dto.getRemark());
    }

    private FixedAssetDisposalDTO toDTO(FixedAssetDisposal entity) {
        FixedAssetDisposalDTO dto = new FixedAssetDisposalDTO();
        dto.setId(entity.getId());
        dto.setDisposalNo(entity.getDisposalNo());
        dto.setAssetId(entity.getAssetId());
        dto.setAssetCode(entity.getAssetCode());
        dto.setAssetName(entity.getAssetName());
        dto.setDisposalDate(entity.getDisposalDate());
        dto.setDisposalType(entity.getDisposalType());
        dto.setDisposalAmount(entity.getDisposalAmount());
        dto.setNetValue(entity.getNetValue());
        dto.setGainLoss(entity.getGainLoss());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setApprovalComment(entity.getApprovalComment());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
