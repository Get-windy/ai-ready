package cn.aiedge.erp.signature.service.impl;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.DeliveryRating;
import cn.aiedge.erp.signature.mapper.DeliveryRatingMapper;
import cn.aiedge.erp.signature.service.RatingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final DeliveryRatingMapper ratingMapper;

    @Override
    @Transactional
    public DeliveryRating createRating(RatingCreateRequest request) {
        if (request.getRatingScore() == null || request.getRatingScore() < 1 || request.getRatingScore() > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        DeliveryRating rating = new DeliveryRating();
        rating.setSignatureId(request.getSignatureId());
        rating.setOrderNo(request.getOrderNo());
        rating.setRatingScore(request.getRatingScore());
        rating.setRatingContent(request.getRatingContent());
        rating.setRatingTags(request.getRatingTags());
        rating.setRatingImages(request.getRatingImages());
        rating.setCustomerId(request.getCustomerId());
        rating.setCustomerName(request.getCustomerName());
        rating.setRatingTime(LocalDateTime.now());
        rating.setStatus(1);
        ratingMapper.insert(rating);
        return rating;
    }

    @Override
    public DeliveryRating getRatingById(Long id) {
        return ratingMapper.selectById(id);
    }

    @Override
    public DeliveryRating getRatingBySignatureId(Long signatureId) {
        LambdaQueryWrapper<DeliveryRating> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRating::getSignatureId, signatureId);
        return ratingMapper.selectOne(wrapper);
    }

    @Override
    public Page<DeliveryRating> listRatings(Integer page, Integer size, Long deliveryPersonId) {
        Page<DeliveryRating> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<DeliveryRating> wrapper = new LambdaQueryWrapper<>();
        if (deliveryPersonId != null) {
            wrapper.eq(DeliveryRating::getDeliveryPersonId, deliveryPersonId);
        }
        wrapper.orderByDesc(DeliveryRating::getRatingTime);
        return ratingMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Map<String, Object> getRatingStats(Long deliveryPersonId) {
        Map<String, Object> stats = ratingMapper.selectRatingStatsByDeliveryPerson(deliveryPersonId);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("avg_score", 0.0);
            stats.put("total_count", 0L);
        }

        List<Map<String, Object>> distribution = ratingMapper.selectRatingDistribution(deliveryPersonId);
        Map<String, Long> distributionMap = new HashMap<>();
        for (Map<String, Object> item : distribution) {
            Integer score = (Integer) item.get("rating_score");
            Long count = ((Number) item.get("count")).longValue();
            distributionMap.put(score + "星", count);
        }
        stats.put("distribution", distributionMap);

        return stats;
    }
}