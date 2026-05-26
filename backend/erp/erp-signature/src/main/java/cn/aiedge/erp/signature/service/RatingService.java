package cn.aiedge.erp.signature.service;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.DeliveryRating;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Map;

public interface RatingService {

    DeliveryRating createRating(RatingCreateRequest request);

    DeliveryRating getRatingById(Long id);

    DeliveryRating getRatingBySignatureId(Long signatureId);

    Page<DeliveryRating> listRatings(Integer page, Integer size, Long deliveryPersonId);

    Map<String, Object> getRatingStats(Long deliveryPersonId);
}