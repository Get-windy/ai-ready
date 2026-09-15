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

    // ── V8.1.0 新增商品表单字段 ─

    /** 所属行业类别 */
    private String industryCategory;

    /** 近效期天数 */
    private Integer nearExpiryDays;

    /** 型号 */
    private String model;

    /** 货号(别名,区别于系统自动编码) */
    private String productCodeAlias;

    /** 是否启用保质期/批次号 0=否 1=是 */
    private Integer isBatchExpiryManaged;

    /** 标品认定 1=标品 0=非标品 */
    private Integer isStandardProduct;

    /** 使用优惠券 0=否 1=是 */
    private Integer useCoupon;

    /** 销售常用单位ID(关联erp_product_unit) */
    private Long defaultSalesUnitId;

    /** 采购常用单位ID(关联erp_product_unit) */
    private Long defaultPurchaseUnitId;

    /** 库存单位ID(关联erp_product_unit) */
    private Long defaultStockUnitId;

    /** 商品详情页富文本内容(商城) */
    private String richTextDetail;

    /** 商城显示标题 */
    private String mallDisplayTitle;

    /** 商城商品描述 */
    private String mallDescription;

    /**
     * 商品标签：逗号分隔的**标准槽位编码**（如 TAG_1,TAG_5），对应 erp_mall_tag.tag_code。
     * 槽位显示名（昵称）可在「商品辅助资料 → 商品标签」自定义，改昵称不影响已打标商品。
     */
    private String mallTags;

    /** 商城上架状态 0=下架 1=上架 */
    private Integer mallShelfStatus;

    /** 商城排序值 */
    private Integer mallSortOrder;

    /** 商城起订量 */
    private Integer mallMinOrderQty;

    /** 商城限购量(0=不限) */
    private Integer mallPurchaseLimit;

    /** 主图视频URL */
    private String videoUrl;

    /** 商城排序方式 DEFAULT=默认 SALES=按销量 MANUAL=手动排序 */
    private String mallSortType;

    /** 商品积分 */
    private java.math.BigDecimal mallPoints;

    /** 商城检索关键字 */
    private String keywords;

    /** 单位显示开关: 0=隐藏 1=显示（商城「单位显示」页） */
    @TableField("unit_display")
    private Integer unitDisplay;

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

    /** 等级价格映射：key=等级编码(GRADE_1等)，value=价格 */
    @TableField(exist = false)
    private java.util.Map<String, java.math.BigDecimal> gradePriceMap;

    /** 可用库存(Σ erp_stock.available_quantity，列表展示用) */
    @TableField(exist = false)
    private java.math.BigDecimal availableStock;

    /** 换算关系描述，如 "1箱=12袋"(列表展示用) */
    @TableField(exist = false)
    private String conversionRelation;

    /** 默认仓库（库存最多的仓库，列表展示用） */
    @TableField(exist = false)
    private String defaultWarehouseName;

    /** 商品标签列表(由 mallTags 逗号串拆分，列表展示用) */
    @TableField(exist = false)
    private java.util.List<String> mallTagList;

    // ── 单位级派生字段（来自 erp_product_unit，列表展示用；非 erp_product 物理列） ──

    /**
     * 「预设进价」列表展示值（商城「单位显示」页列，默认隐藏）。
     *
     * <p>取数口径：**基本单位**（is_base_unit=1）的 erp_product_unit.preset_purchase_price；
     * 若该商品无基本单位行，则取 sort_order 最小的单位行；仍为空时回退商品级
     * erp_product.purchase_price（导入/档案维护写入的商品级预设进价）。</p>
     */
    @TableField(exist = false)
    private java.math.BigDecimal presetPurchasePrice;

    /**
     * 单位显示类型（**单位粒度**）列表展示值：**基本单位**的
     * erp_product_unit.unit_display_type，取值 {@code "-1"}=全部 / {@code "0"}=只显示常用单位 /
     * {@code "1"}=只显示小单位 / {@code "2"}=只显示中/大单位 / {@code null}=未显式设置。
     *
     * <p>取值依据 2026-09-14 对标 ql361「单位显示类型」下拉 DOM 实测，
     * 见 Flyway V11.361.9（改正）与 V11.361.8（建列）。</p>
     *
     * <p>多单位口径：本页是商品行列表，以基本单位为代表值回显；各单位的真实取值
     * 仍逐行独立存储（可被商品表单按单位单独设置），NULL 时前端按商品级
     * unit_display 兜底显示。</p>
     */
    @TableField(exist = false)
    private String unitDisplayType;
}
