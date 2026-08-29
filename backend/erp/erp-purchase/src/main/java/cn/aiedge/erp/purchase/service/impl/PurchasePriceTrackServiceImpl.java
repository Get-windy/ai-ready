package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrackQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrackSaveDTO;
import cn.aiedge.erp.purchase.dto.PurchasePriceTrendVO;
import cn.aiedge.erp.purchase.entity.PurchasePriceTrack;
import cn.aiedge.erp.purchase.mapper.PurchasePriceTrackMapper;
import cn.aiedge.erp.purchase.service.PurchasePriceTrackService;
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
 * 采购价格跟踪ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchasePriceTrackServiceImpl extends ServiceImpl<PurchasePriceTrackMapper, PurchasePriceTrack>
        implements PurchasePriceTrackService {

    private final PurchasePriceTrackMapper priceTrackMapper;

    @Override
    public IPage<PurchasePriceTrack> page(PurchasePriceTrackQueryDTO dto) {
        long current = dto.getCurrent() != null && dto.getCurrent() > 0 ? dto.getCurrent() : 1L;
        long size = dto.getSize() != null && dto.getSize() > 0 ? dto.getSize() : 20L;
        return priceTrackMapper.selectTrackPage(new Page<>(current, size),
                dto.getProductName(), dto.getPartnerName(),
                dto.getStartDate(), dto.getEndDate(),
                dto.getCategoryId(), dto.getUnitType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchasePriceTrack saveTrack(PurchasePriceTrackSaveDTO dto) {
        if (dto.getPurchasePrice() == null) {
            throw new BusinessException("采购价格不能为空");
        }
        LocalDate purchaseDate = dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now();

        // 若同 商品×往来单位×采购日期 已存在，则更新该记录（避免重复价格点）
        PurchasePriceTrack existing = getOne(new LambdaQueryWrapper<PurchasePriceTrack>()
                .eq(PurchasePriceTrack::getProductId, dto.getProductId())
                .eq(PurchasePriceTrack::getPartnerId, dto.getPartnerId())
                .eq(PurchasePriceTrack::getPurchaseDate, purchaseDate), false);

        if (existing != null) {
            applySnapshot(existing, dto);
            existing.setPurchasePrice(dto.getPurchasePrice());
            existing.setPurchaseDate(purchaseDate);
            existing.setLastModifyTime(LocalDateTime.now());
            existing.setSource("MANUAL");
            updateById(existing);
            return existing;
        }

        PurchasePriceTrack track = new PurchasePriceTrack();
        track.setTenantId(resolveTenantId());
        track.setProductId(dto.getProductId());
        track.setPartnerId(dto.getPartnerId());
        applySnapshot(track, dto);
        track.setUnit(dto.getUnit());
        track.setPurchasePrice(dto.getPurchasePrice());
        track.setPurchaseDate(purchaseDate);
        track.setLastModifyTime(LocalDateTime.now());
        track.setSource("MANUAL");
        track.setDeleted(0);
        save(track);
        return track;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchasePriceTrack updateTrack(Long id, PurchasePriceTrackSaveDTO dto) {
        PurchasePriceTrack track = getById(id);
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
        if (dto.getUnit() != null) {
            track.setUnit(dto.getUnit());
        }
        if (dto.getPurchasePrice() != null) {
            track.setPurchasePrice(dto.getPurchasePrice());
        }
        if (dto.getPurchaseDate() != null) {
            track.setPurchaseDate(dto.getPurchaseDate());
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
    public List<PurchasePriceTrendVO> trend(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return priceTrackMapper.selectTrend(productId);
    }

    // ═══════════════════════════════════════════════════════════════
    // 私有辅助
    // ═══════════════════════════════════════════════════════════════
    private void applySnapshot(PurchasePriceTrack track, PurchasePriceTrackSaveDTO dto) {
        // 商品快照：优先使用前端传入值，缺省时从商品主数据回填
        if (dto.getProductId() != null) {
            track.setProductId(dto.getProductId());
            if (hasText(dto.getProductCode())) track.setProductCode(dto.getProductCode());
            if (hasText(dto.getProductName())) track.setProductName(dto.getProductName());
            if (hasText(dto.getItemCode())) track.setItemCode(dto.getItemCode());
            if (hasText(dto.getSpecification())) track.setSpecification(dto.getSpecification());
            if (hasText(dto.getModel())) track.setModel(dto.getModel());
            if (hasText(dto.getOrigin())) track.setOrigin(dto.getOrigin());
        }
        Map<String, Object> product = priceTrackMapper.selectProductSnapshot(dto.getProductId());
        if (product != null) {
            if (!hasText(track.getProductCode())) track.setProductCode(str(product.get("product_code")));
            if (!hasText(track.getProductName())) track.setProductName(str(product.get("product_name")));
            if (!hasText(track.getItemCode())) track.setItemCode(str(product.get("item_code")));
            if (dto.getUnit() == null) {
                track.setUnit(str(product.get("unit")));
            }
            if (!hasText(track.getSpecification())) track.setSpecification(str(product.get("specification")));
            if (!hasText(track.getModel())) track.setModel(str(product.get("model")));
            if (!hasText(track.getOrigin())) track.setOrigin(str(product.get("origin")));
            if (!hasText(track.getBarcode())) track.setBarcode(str(product.get("barcode")));
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
