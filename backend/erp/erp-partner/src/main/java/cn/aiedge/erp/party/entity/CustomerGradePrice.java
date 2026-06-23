package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_customer_grade_price")
public class CustomerGradePrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gradeId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String productSpec;

    private BigDecimal standardPrice;

    private BigDecimal gradePrice;

    private Integer priceType;

    private BigDecimal discountRate;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private LocalDateTime effectiveTime;

    private LocalDateTime expireTime;

    private Boolean isDefault;

    private Integer priority;

    private String remark;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
