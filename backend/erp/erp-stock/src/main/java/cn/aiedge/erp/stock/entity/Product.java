package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product")
public class Product {

    /**
     * 产品ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 单位
     */
    private String unit;

    /**
     * 分类(旧版文本字段,兼容保留)
     */
    private String category;

    /**
     * 分类ID(新版关联erp_product_category)
     */
    private Long categoryId;

    /**
     * 规格
     */
    private String spec;

    /**
     * 默认产品等级ID
     */
    private Long productGradeId;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 标准售价
     */
    private BigDecimal standardPrice;

    /**
     * 批发价
     */
    private BigDecimal wholesalePrice;

    /**
     * 产品图片URL
     */
    private String imageUrl;

    /**
     * 条形码
     */
    private String barcode;

    /**
     * 产品类型 SINGLE单品/KIT套件/SERVICE服务
     */
    private String productType;

    /**
     * SKU（库存单位编码）
     */
    private String sku;

    /**
     * 是否已配置等级价格 0=未配置 1=已配置
     */
    private Integer hasGradePrice;

    // ── V3.0.0 扩展字段 ──

    private java.math.BigDecimal weight;

    private java.math.BigDecimal volume;

    private String origin;

    private String brand;

    private java.math.BigDecimal taxRate;

    private java.math.BigDecimal purchasePrice;

    private java.math.BigDecimal retailPrice;

    private Integer shelfLifeDays;

    private Integer isBatchManaged;

    private Integer isSerialManaged;

    private String approvalStatus;

    private Long approvalBy;

    private java.time.LocalDateTime approvalTime;

    /**
     * 状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    // ── 非持久化字段(用于前端展示) ──

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String gradeName;
}
