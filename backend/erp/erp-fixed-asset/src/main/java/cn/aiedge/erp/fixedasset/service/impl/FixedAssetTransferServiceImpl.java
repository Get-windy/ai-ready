package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetTransferDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetTransfer;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetTransferRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetTransferService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 固定资产转移服务实现
 */
@Service
@RequiredArgsConstructor
public class FixedAssetTransferServiceImpl implements FixedAssetTransferService {

    private final FixedAssetTransferRepository transferRepository;
    private final FixedAssetRepository fixedAssetRepository;

    @Override
    @Transactional
    public FixedAssetTransferDTO create(FixedAssetTransferDTO dto) {
        FixedAssetTransfer entity = toEntity(dto);
        if (entity.getTransferNo() == null || entity.getTransferNo().isBlank()) {
            entity.setTransferNo("FT" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        }
        entity.setStatus("draft");
        entity.setTransferTime(LocalDateTime.now());
        entity.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = transferRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetTransferDTO update(Long id, FixedAssetTransferDTO dto) {
        FixedAssetTransfer entity = transferRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("转移记录不存在: " + id));
        updateEntity(entity, dto);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = transferRepository.save(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FixedAssetTransfer entity = transferRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("转移记录不存在: " + id));
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity.markAsDeleted();
        transferRepository.save(entity);
    }

    @Override
    public FixedAssetTransferDTO getById(Long id) {
        FixedAssetTransfer entity = transferRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("转移记录不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public Page<FixedAssetTransferDTO> getPage(String transferNo, String status, Pageable pageable) {
        Specification<FixedAssetTransfer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (StringUtils.hasText(transferNo)) {
                predicates.add(cb.like(root.get("transferNo"), "%" + transferNo + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAssetTransfer> page = transferRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public FixedAssetTransferDTO approve(Long id, String comment) {
        FixedAssetTransfer entity = transferRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("转移记录不存在: " + id));
        entity.setStatus("approved");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = transferRepository.save(entity);

        // Update asset department and custodian
        if (entity.getAssetId() != null) {
            String toDeptId = entity.getToDepartmentId();
            String toDeptName = entity.getToDepartmentName();
            String toCustId = entity.getToCustodianId();
            String toCustName = entity.getToCustodianName();
            fixedAssetRepository.findById(entity.getAssetId()).ifPresent(asset -> {
                asset.setDepartmentId(toDeptId);
                asset.setDepartmentName(toDeptName);
                asset.setCustodianId(toCustId);
                asset.setCustodianName(toCustName);
                asset.setStatus("transferred");
                fixedAssetRepository.save(asset);
            });
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public FixedAssetTransferDTO reject(Long id, String comment) {
        FixedAssetTransfer entity = transferRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("转移记录不存在: " + id));
        entity.setStatus("rejected");
        entity.setApprovalComment(comment);
        entity.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
        entity = transferRepository.save(entity);
        return toDTO(entity);
    }

    private FixedAssetTransfer toEntity(FixedAssetTransferDTO dto) {
        FixedAssetTransfer entity = new FixedAssetTransfer();
        updateEntity(entity, dto);
        return entity;
    }

    private void updateEntity(FixedAssetTransfer entity, FixedAssetTransferDTO dto) {
        entity.setTransferNo(dto.getTransferNo());
        entity.setAssetId(dto.getAssetId());
        entity.setAssetCode(dto.getAssetCode());
        entity.setAssetName(dto.getAssetName());
        entity.setFromDepartmentId(dto.getFromDepartmentId());
        entity.setFromDepartmentName(dto.getFromDepartmentName());
        entity.setToDepartmentId(dto.getToDepartmentId());
        entity.setToDepartmentName(dto.getToDepartmentName());
        entity.setFromCustodianId(dto.getFromCustodianId());
        entity.setFromCustodianName(dto.getFromCustodianName());
        entity.setToCustodianId(dto.getToCustodianId());
        entity.setToCustodianName(dto.getToCustodianName());
        entity.setTransferDate(dto.getTransferDate());
        entity.setReason(dto.getReason());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setApprovalComment(dto.getApprovalComment());
        entity.setTransferTime(dto.getTransferTime());
        entity.setRemark(dto.getRemark());
    }

    private FixedAssetTransferDTO toDTO(FixedAssetTransfer entity) {
        FixedAssetTransferDTO dto = new FixedAssetTransferDTO();
        dto.setId(entity.getId());
        dto.setTransferNo(entity.getTransferNo());
        dto.setAssetId(entity.getAssetId());
        dto.setAssetCode(entity.getAssetCode());
        dto.setAssetName(entity.getAssetName());
        dto.setFromDepartmentId(entity.getFromDepartmentId());
        dto.setFromDepartmentName(entity.getFromDepartmentName());
        dto.setToDepartmentId(entity.getToDepartmentId());
        dto.setToDepartmentName(entity.getToDepartmentName());
        dto.setFromCustodianId(entity.getFromCustodianId());
        dto.setFromCustodianName(entity.getFromCustodianName());
        dto.setToCustodianId(entity.getToCustodianId());
        dto.setToCustodianName(entity.getToCustodianName());
        dto.setTransferDate(entity.getTransferDate());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setApprovalComment(entity.getApprovalComment());
        dto.setTransferTime(entity.getTransferTime());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
