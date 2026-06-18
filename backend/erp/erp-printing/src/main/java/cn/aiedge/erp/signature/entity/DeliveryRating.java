package cn.aiedge.erp.signature.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_delivery_rating")
public class DeliveryRating {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long signatureId;

    private String orderNo;

    private Integer ratingScore;

    private String ratingContent;

    private String ratingTags;

    private String ratingImages;

    private Long customerId;

    private String customerName;

    private Long deliveryPersonId;

    private String deliveryPersonName;

    private LocalDateTime ratingTime;

    private Integer status;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}