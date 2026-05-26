package cn.aiedge.erp.signature.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryRatingDTO {

    private Long id;

    private Long signatureId;

    private String orderNo;

    private Integer ratingScore;

    private String ratingContent;

    private String ratingTags;

    private String ratingImages;

    private String customerName;

    private String deliveryPersonName;

    private LocalDateTime ratingTime;
}