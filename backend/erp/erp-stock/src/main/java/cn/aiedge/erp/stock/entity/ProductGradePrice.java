package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品等级价格实体-每个产品在不同等级下的价格
 *
 * @author AI-Ready Team
 * @since 1.5.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_grade_price")
public class ProductGradePrice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 产品ID */
    private Long productId;

    /** 产品等级ID */
    private Long productGradeId;

    /** 该等级对应价格 */
    private BigDecimal price;

    /** 最低起订量 */
    private Integer minOrderQty;

    /** 是否生效: 1生效 */
    private Integer isActive;

    /** 生效日期 */
    private LocalDateTime effectiveDate;

    /** 失效日期 */
    private LocalDateTime expireDate;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ── 非持久化字段 ──

    /** 等级名称(关联查询) */
    @TableField(exist = false)
    private String gradeName;

    /** 等级编码 */
    @TableField(exist = false)
    private String gradeCode;

    /** 产品名称 */
    @TableField(exist = false)
    private String productName;
}
