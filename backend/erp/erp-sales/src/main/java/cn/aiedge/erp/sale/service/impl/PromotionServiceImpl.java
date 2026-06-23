package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.sale.dto.PromotionActivityDTO;
import cn.aiedge.erp.sale.entity.PromotionActivity;
import cn.aiedge.erp.sale.mapper.PromotionActivityMapper;
import cn.aiedge.erp.sale.service.IPromotionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 促销活动服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl extends ServiceImpl<PromotionActivityMapper, PromotionActivity>
        implements IPromotionService {

    private final PromotionActivityMapper promotionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPromotion(PromotionActivityDTO dto) {
        PromotionActivity activity = new PromotionActivity();
        BeanUtils.copyProperties(dto, activity);
        activity.setStatus("draft");

        // 转换列表为逗号分隔字符串
        if (!CollectionUtils.isEmpty(dto.getCustomerLevels())) {
            activity.setCustomerLevels(String.join(",", dto.getCustomerLevels()));
        }
        if (!CollectionUtils.isEmpty(dto.getProductIds())) {
            activity.setProductIds(dto.getProductIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (!CollectionUtils.isEmpty(dto.getRegions())) {
            activity.setRegions(String.join(",", dto.getRegions()));
        }

        promotionMapper.insert(activity);
        return activity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePromotion(Long id, PromotionActivityDTO dto) {
        PromotionActivity activity = promotionMapper.selectById(id);
        if (activity == null) {
            throw BusinessException.notFound("促销活动不存在");
        }
        BeanUtils.copyProperties(dto, activity, "id", "createTime", "createBy", "status");

        // 转换列表为逗号分隔字符串
        if (!CollectionUtils.isEmpty(dto.getCustomerLevels())) {
            activity.setCustomerLevels(String.join(",", dto.getCustomerLevels()));
        }
        if (!CollectionUtils.isEmpty(dto.getProductIds())) {
            activity.setProductIds(dto.getProductIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (!CollectionUtils.isEmpty(dto.getRegions())) {
            activity.setRegions(String.join(",", dto.getRegions()));
        }

        promotionMapper.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotion(Long id) {
        promotionMapper.deleteById(id);
    }

    @Override
    public PromotionActivityDTO getPromotionDetail(Long id) {
        PromotionActivity activity = promotionMapper.selectById(id);
        if (activity == null) {
            return null;
        }
        return convertToDTO(activity);
    }

    @Override
    public Page<PromotionActivityDTO> pagePromotions(Page<PromotionActivityDTO> page, Long tenantId,
                                                     String name, String status, String type) {
        LambdaQueryWrapper<PromotionActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, PromotionActivity::getTenantId, tenantId)
                .like(StringUtils.hasText(name), PromotionActivity::getName, name)
                .eq(StringUtils.hasText(status), PromotionActivity::getStatus, status)
                .eq(StringUtils.hasText(type), PromotionActivity::getType, type)
                .orderByDesc(PromotionActivity::getCreateTime);

        Page<PromotionActivity> entityPage = new Page<>(page.getCurrent(), page.getSize());
        Page<PromotionActivity> resultPage = promotionMapper.selectPage(entityPage, wrapper);

        List<PromotionActivityDTO> records = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<PromotionActivityDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPromotion(Long id) {
        PromotionActivity activity = promotionMapper.selectById(id);
        if (activity == null) {
            throw BusinessException.notFound("促销活动不存在");
        }
        if (!"draft".equals(activity.getStatus()) && !"cancelled".equals(activity.getStatus())) {
            throw BusinessException.badRequest("只有草稿或已取消状态的促销可以发布");
        }
        activity.setStatus("published");
        promotionMapper.updateById(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPromotion(Long id) {
        PromotionActivity activity = promotionMapper.selectById(id);
        if (activity == null) {
            throw BusinessException.notFound("促销活动不存在");
        }
        if (!"published".equals(activity.getStatus())) {
            throw BusinessException.badRequest("只有已发布状态的促销可以取消");
        }
        activity.setStatus("cancelled");
        promotionMapper.updateById(activity);
    }

    @Override
    public List<PromotionActivityDTO> getActivePromotions(Long tenantId) {
        List<PromotionActivity> activities = promotionMapper.selectActivePromotions(tenantId, LocalDateTime.now());
        return activities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PromotionActivityDTO> getApplicablePromotions(Long tenantId, Long productId, String customerLevel) {
        List<PromotionActivity> activities = promotionMapper.selectApplicablePromotions(
                tenantId, productId, customerLevel, LocalDateTime.now());
        return activities.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private PromotionActivityDTO convertToDTO(PromotionActivity activity) {
        PromotionActivityDTO dto = new PromotionActivityDTO();
        BeanUtils.copyProperties(activity, dto);

        // 转换逗号分隔字符串为列表
        if (StringUtils.hasText(activity.getCustomerLevels())) {
            dto.setCustomerLevels(Arrays.asList(activity.getCustomerLevels().split(",")));
        } else {
            dto.setCustomerLevels(Collections.emptyList());
        }
        if (StringUtils.hasText(activity.getProductIds())) {
            dto.setProductIds(Arrays.stream(activity.getProductIds().split(","))
                    .map(Long::valueOf).collect(Collectors.toList()));
        } else {
            dto.setProductIds(Collections.emptyList());
        }
        if (StringUtils.hasText(activity.getRegions())) {
            dto.setRegions(Arrays.asList(activity.getRegions().split(",")));
        } else {
            dto.setRegions(Collections.emptyList());
        }
        return dto;
    }
}
