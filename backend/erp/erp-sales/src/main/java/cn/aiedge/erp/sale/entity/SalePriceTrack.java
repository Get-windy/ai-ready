package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售价格跟踪实体
 * <p>
 * 一条记录代表"某个往来单位（客户）销售某个商品"的一次价格点。
 * 列表按 product_id + partner_id 分组取最近一条；趋势按 product_id 取全部历史。
 * 数据来源：销售出库明细（初始化种子）+ 手动维护（价格折扣新增/修改）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_price_track")
public class SalePriceTrack {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    // ═══ 商品信息快照 ═══
    /** 商品ID */
    private Long productId;
    /** 货号 */
    private String productCode;
    /** 商品名称 */
    private String productName;
    /** 商品单位 */
    private String unit;
    /** 条码 */
    private String barcode;
    /** 规格 */
    private String specification;
    /** 型号 */
    private String model;
    /** 产地 */
    private String origin;

    // ═══ 往来单位（客户）信息快照 ═══
    /** 往来单位ID */
    private Long partnerId;
    /** 往来单位编号 */
    private String partnerCode;
    /** 往来单位名称 */
    private String partnerName;

    // ═══ 销售价格 ═══
    /** 最近销售价 */
    @TableField("sale_price")
    private BigDecimal salePrice;
    /** 最近销售折扣（%） */
    @TableField("discount_rate")
    private BigDecimal discountRate;
    /** 最近销售日期 */
    @TableField("sale_date")
    private LocalDate saleDate;
    /** 最后修改时间 */
    @TableField("last_modify_time")
    private LocalDateTime lastModifyTime;

    // ═══ 来源信息 ═══
    /** 来源：MANUAL（手动维护）/ INIT（出库明细初始化） */
    private String source;
    /** 备注 */
    private String remark;

    // ═══ 系统字段 ═══
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
