package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.dto.SalePriceTrackQueryDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrackSaveDTO;
import cn.aiedge.erp.sale.dto.SalePriceTrendVO;
import cn.aiedge.erp.sale.entity.SalePriceTrack;
import cn.aiedge.erp.sale.mapper.SalePriceTrackMapper;
import cn.aiedge.erp.sale.service.SalePriceTrackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 销售价格跟踪ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalePriceTrackServiceImpl extends ServiceImpl<SalePriceTrackMapper, SalePriceTrack>
        implements SalePriceTrackService {

    private final SalePriceTrackMapper priceTrackMapper;

    @Override
    public IPage<SalePriceTrack> page(SalePriceTrackQueryDTO dto) {
        long current = dto.getCurrent() != null && dto.getCurrent() > 0 ? dto.getCurrent() : 1L;
        long size = dto.getSize() != null && dto.getSize() > 0 ? dto.getSize() : 20L;
        return priceTrackMapper.selectTrackPage(new Page<>(current, size),
                dto.getProductName(), dto.getProductCode(), dto.getBarcode(), dto.getPartnerName(),
                dto.getStartDate(), dto.getEndDate(),
                dto.getCategoryId(), dto.getUnitType(),
                dto.getOnlyDiscounted(), dto.getOnlyHasSale());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalePriceTrack saveTrack(SalePriceTrackSaveDTO dto) {
        if (dto.getSalePrice() == null) {
            throw new BusinessException("销售价不能为空");
        }
        LocalDate saleDate = dto.getSaleDate() != null ? dto.getSaleDate() : LocalDate.now();

        // 若同 商品×往来单位×销售日期 已存在，则更新该记录（避免重复价格点）
        SalePriceTrack existing = getOne(new LambdaQueryWrapper<SalePriceTrack>()
                .eq(SalePriceTrack::getProductId, dto.getProductId())
                .eq(SalePriceTrack::getPartnerId, dto.getPartnerId())
                .eq(SalePriceTrack::getSaleDate, saleDate), false);

        if (existing != null) {
            applySnapshot(existing, dto);
            existing.setSalePrice(dto.getSalePrice());
            existing.setDiscountRate(normalizeDiscount(dto.getDiscountRate()));
            existing.setSaleDate(saleDate);
            existing.setLastModifyTime(LocalDateTime.now());
            existing.setSource("MANUAL");
            updateById(existing);
            return existing;
        }

        SalePriceTrack track = new SalePriceTrack();
        track.setTenantId(resolveTenantId());
        track.setProductId(dto.getProductId());
        track.setPartnerId(dto.getPartnerId());
        applySnapshot(track, dto);
        track.setSalePrice(dto.getSalePrice());
        track.setDiscountRate(normalizeDiscount(dto.getDiscountRate()));
        track.setSaleDate(saleDate);
        track.setLastModifyTime(LocalDateTime.now());
        track.setSource("MANUAL");
        track.setDeleted(0);
        save(track);
        return track;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalePriceTrack updateTrack(Long id, SalePriceTrackSaveDTO dto) {
        SalePriceTrack track = getById(id);
        if (track == null) {
            throw new BusinessException("价格记录不存在");
        }
        if (dto.getProductId() != null) {
            track.setProductId(dto.getProductId());
        }
        if (dto.getPartnerId() != null) {
            track.setPartnerId(dto.getPartnerId());
        }
        applySnapshot(track, dto);
        if (dto.getSalePrice() != null) {
            track.setSalePrice(dto.getSalePrice());
        }
        if (dto.getDiscountRate() != null) {
            track.setDiscountRate(normalizeDiscount(dto.getDiscountRate()));
        }
        if (dto.getSaleDate() != null) {
            track.setSaleDate(dto.getSaleDate());
        }
        track.setLastModifyTime(LocalDateTime.now());
        track.setSource("MANUAL");
        updateById(track);
        return track;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrack(Long id) {
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        removeByIds(ids);
    }

    @Override
    public List<SalePriceTrendVO> trend(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return priceTrackMapper.selectTrend(productId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 私有辅助
    // ═══════════════════════════════════════════════════════════════
    private void applySnapshot(SalePriceTrack track, SalePriceTrackSaveDTO dto) {
        // 商品快照：优先使用前端传入值，缺省时从商品主数据回填
        if (dto.getProductId() != null) {
            track.setProductId(dto.getProductId());
            if (hasText(dto.getProductCode())) track.setProductCode(dto.getProductCode());
            if (hasText(dto.getProductName())) track.setProductName(dto.getProductName());
            if (hasText(dto.getUnit())) track.setUnit(dto.getUnit());
            if (hasText(dto.getBarcode())) track.setBarcode(dto.getBarcode());
            if (hasText(dto.getSpecification())) track.setSpecification(dto.getSpecification());
            if (hasText(dto.getModel())) track.setModel(dto.getModel());
            if (hasText(dto.getOrigin())) track.setOrigin(dto.getOrigin());
        }
        Map<String, Object> product = priceTrackMapper.selectProductSnapshot(dto.getProductId());
        if (product != null) {
            if (!hasText(track.getProductCode())) track.setProductCode(str(product.get("product_code")));
            if (!hasText(track.getProductName())) track.setProductName(str(product.get("product_name")));
            if (!hasText(track.getUnit())) track.setUnit(str(product.get("unit")));
            if (!hasText(track.getBarcode())) track.setBarcode(str(product.get("barcode")));
            if (!hasText(track.getSpecification())) track.setSpecification(str(product.get("specification")));
            if (!hasText(track.getModel())) track.setModel(str(product.get("model")));
            if (!hasText(track.getOrigin())) track.setOrigin(str(product.get("origin")));
        }
        // 往来单位快照：优先使用前端传入值，缺省时从往来单位主数据回填
        if (dto.getPartnerId() != null) {
            track.setPartnerId(dto.getPartnerId());
            if (hasText(dto.getPartnerCode())) track.setPartnerCode(dto.getPartnerCode());
            if (hasText(dto.getPartnerName())) track.setPartnerName(dto.getPartnerName());
        }
        Map<String, Object> partner = priceTrackMapper.selectPartnerSnapshot(dto.getPartnerId());
        if (partner != null) {
            if (!hasText(track.getPartnerCode())) track.setPartnerCode(str(partner.get("partner_code")));
            if (!hasText(track.getPartnerName())) track.setPartnerName(str(partner.get("partner_name")));
        }
    }

    private boolean hasText(String v) {
        return v != null && !v.isBlank();
    }

    /**
     * 折扣率归一化：本系统出库明细以 0（或空）表示"未打折"，
     * 价格跟踪统一按对标口径存 100（%）= 原价成交。
     */
    private BigDecimal normalizeDiscount(BigDecimal v) {
        BigDecimal hundred = new BigDecimal("100");
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0 || v.compareTo(hundred) >= 0) {
            return hundred;
        }
        return v;
    }

    private Long resolveTenantId() {
        try {
            Long tid = MyBatisPlusConfig.getCurrentTenantIdValue();
            return tid != null ? tid : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
