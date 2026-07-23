package cn.aiedge.erp.fixedasset.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.fixedasset.dto.FixedAssetDepreciationDTO;
import cn.aiedge.erp.fixedasset.model.FixedAsset;
import cn.aiedge.erp.fixedasset.model.FixedAssetDepreciation;
import cn.aiedge.erp.fixedasset.repository.FixedAssetDepreciationRepository;
import cn.aiedge.erp.fixedasset.repository.FixedAssetRepository;
import cn.aiedge.erp.fixedasset.service.FixedAssetDepreciationService;
import cn.aiedge.erp.fixedasset.service.depreciation.DepreciationContext;
import cn.aiedge.erp.fixedasset.service.integration.FixedAssetAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 固定资产折旧服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetDepreciationServiceImpl implements FixedAssetDepreciationService {

    private final FixedAssetDepreciationRepository depreciationRepository;
    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationContext depreciationContext;
    private final FixedAssetAccountingService fixedAssetAccountingService;

    @Override
    public FixedAssetDepreciationDTO getById(Long id) {
        FixedAssetDepreciation entity = depreciationRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("折旧记录不存在: " + id));
        return toDTO(entity);
    }

    @Override
    public Page<FixedAssetDepreciationDTO> getPage(Long assetId, String period, Pageable pageable) {
        Specification<FixedAssetDepreciation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (assetId != null) {
                predicates.add(cb.equal(root.get("assetId"), assetId));
            }
            if (StringUtils.hasText(period)) {
                predicates.add(cb.like(root.get("period"), period + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<FixedAssetDepreciation> page = depreciationRepository.findAll(spec, pageable);
        return page.map(this::toDTO);
    }

    @Override
    @Transactional
    public List<FixedAssetDepreciationDTO> batchCalculate() {
        List<FixedAsset> activeAssets = fixedAssetRepository.findByStatus("active");
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<FixedAssetDepreciationDTO> results = new ArrayList<>();

        for (FixedAsset asset : activeAssets) {
            // 防重复计提：本期间已有计提完成记录则跳过（同时防止重复生成折旧凭证）
            boolean alreadyDepreciated = depreciationRepository
                .findByAssetIdAndPeriodOrderByCreatedAtAsc(asset.getId(), period)
                .stream().anyMatch(d -> "completed".equals(d.getStatus()));
            if (alreadyDepreciated) {
                log.info("资产本期已计提折旧，跳过: assetId={}, period={}", asset.getId(), period);
                continue;
            }
            try {
                Map<String, BigDecimal> result = depreciationContext.calculateDepreciation(asset);

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
                depreciation.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
                depreciation = depreciationRepository.save(depreciation);

                asset.setAccumulatedDepreciation(result.get("accumulatedDepreciation"));
                asset.setNetValue(result.get("netValue"));
                asset.setMonthlyDepreciation(result.get("periodAmount"));
                asset.setUpdatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
                fixedAssetRepository.save(asset);

                // 业财直调：计提成功生成折旧凭证（失败不阻断计提结果，仅记录日志）
                BigDecimal periodAmount = result.get("periodAmount");
                if (periodAmount != null && periodAmount.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        String voucherNo = fixedAssetAccountingService.postDepreciationVoucher(
                            asset.getId(), asset.getAssetCode(), asset.getAssetName(), periodAmount,
                            depreciation.getDepreciationDate().getYear(),
                            depreciation.getDepreciationDate().getMonthValue());
                        depreciation.setRemark("折旧凭证: " + voucherNo);
                    } catch (Exception e) {
                        log.error("折旧凭证生成失败: assetId={}, period={}, 原因={}", asset.getId(), period, e.getMessage(), e);
                    }
                }

                results.add(toDTO(depreciation));
            } catch (Exception e) {
                // Log error and continue
                FixedAssetDepreciation errorRecord = new FixedAssetDepreciation();
                errorRecord.setAssetId(asset.getId());
                errorRecord.setPeriod(period);
                errorRecord.setAssetName(asset.getAssetName());
                errorRecord.setAssetCode(asset.getAssetCode());
                errorRecord.setStatus("failed");
                errorRecord.setCreatedBy(String.valueOf(SecurityUtils.getCurrentUserId()));
                errorRecord = depreciationRepository.save(errorRecord);
                results.add(toDTO(errorRecord));
            }
        }

        return results;
    }

    private FixedAssetDepreciationDTO toDTO(FixedAssetDepreciation entity) {
        FixedAssetDepreciationDTO dto = new FixedAssetDepreciationDTO();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setPeriod(entity.getPeriod());
        dto.setDepreciationDate(entity.getDepreciationDate());
        dto.setPeriodAmount(entity.getPeriodAmount());
        dto.setAccumulatedDepreciation(entity.getAccumulatedDepreciation());
        dto.setNetValue(entity.getNetValue());
        dto.setAssetOriginalValue(entity.getAssetOriginalValue());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetCode(entity.getAssetCode());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
