package cn.aiedge.erp.pricing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_product_grade_price")
public class ProductGradePrice {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long productId;
    private String gradeCode;
    private String gradeName;
    private String priceType;
    private BigDecimal basePrice;
    private BigDecimal gradePrice;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private Integer minOrderQty;
    private Integer maxOrderQty;
    private LocalDateTime effectiveDate;
    private LocalDateTime expireDate;
    private Integer isActive;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}