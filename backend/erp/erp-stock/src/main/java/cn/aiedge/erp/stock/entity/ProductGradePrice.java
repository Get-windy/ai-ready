package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品等级价格（Odoo架构：按产品+单位+等级三维度独立定价）
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_grade_price")
public class ProductGradePrice {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long productId;
    /** 产品单位ID（NULL=产品级别价格，非NULL=按单位定价） */
    private Long unitId;
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

    // ── 限时特价/秒杀 ──
    /** 生效时间（NULL=立即生效），配合 dateEnd 实现限时特价/秒杀 */
    private LocalDateTime dateStart;
    /** 失效时间（NULL=永久有效） */
    private LocalDateTime dateEnd;
    /** 最小数量（NULL=不限），实现数量阶梯定价 */
    private java.math.BigDecimal minQuantity;

    // ── 联系人受益（回扣/提点）──
    /** 受益联系人ID（关联往来单位联系人），NULL=无回扣 */
    private Long contactPartnerId;
    /** 受益模式: DISCOUNT=折扣+提点, MARKUP=基准价+加点, NULL=无 */
    private String benefitType;
    /** 提点/加点比例(%)，如3.00表示3% */
    private BigDecimal benefitRate;
    /** 固定金额，不为NULL时覆盖benefitRate计算 */
    private BigDecimal benefitFixed;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
