package cn.aiedge.erp.signature.dto;

import lombok.Data;

@Data
public class RatingCreateRequest {

    private Long signatureId;

    private String orderNo;

    private Integer ratingScore;

    private String ratingContent;

    private String ratingTags;

    private String ratingImages;

    private Long customerId;

    private String customerName;
}