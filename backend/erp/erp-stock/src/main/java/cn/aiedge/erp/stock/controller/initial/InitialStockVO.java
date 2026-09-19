package cn.aiedge.erp.stock.controller.initial;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 期初库存视图对象（列表行 + 导出行的统一口径）
 * <p>
 * 字段对齐对标 ql361「设置 → 期初录入 → 库存期初」的 10 个数据列：
 * 商品名称 / 货号 / 条码 / 规格 / 型号 / 产地 / 小单位 / 期初数量 / 期初成本单价 / 期初金额。
 * <p>
 * ⚠️ 其中 条码 / 规格 / 型号 / 产地 / 小单位 来自 {@code erp_product}（本页按 product_id 关联商品档案），
 * 期初金额为派生值 {@code quantity * unit_price}（**不落库**，避免「数量/单价改了金额没改」的不一致）。
 * 快照字段（商品名称/货号/仓库）优先取 {@code erp_stock} 自身列，为空时回退商品档案。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InitialStockVO {

    /** 期初行主键（erp_stock.id；雪花ID 由 Jackson 统一序列化为字符串） */
    private Long id;

    /** 商品ID（编辑回填定位用） */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 货号（对标列名「货号」，对应 erp_stock.product_code / erp_product.product_code） */
    private String productCode;

    /** 条码（erp_product.barcode） */
    private String barcode;

    /** 规格（erp_product.spec） */
    private String spec;

    /** 型号（erp_product.model） */
    private String model;

    /** 产地（erp_product.origin） */
    private String origin;

    /** 小单位（优先取库存行单位快照，缺失时回退商品小单位/基础单位） */
    private String unit;

    /** 仓库ID */
    private Long warehouseId;

    /** 仓库名称 */
    private String warehouseName;

    /** 期初数量 */
    private BigDecimal quantity;

    /** 期初成本单价 */
    private BigDecimal unitPrice;

    /** 期初金额 = 期初数量 × 期初成本单价（后端计算，不落库） */
    private BigDecimal amount;

    /** 生产日期（erp_stock.production_date） */
    private LocalDateTime productionDate;

    /** 有效期至（erp_stock.validity_date；注意 DB 列名是 validity_date 而非 expiration_date） */
    private LocalDateTime validityDate;

    /** 备注 */
    private String remark;

    /** 录入时间 */
    private LocalDateTime createTime;
}
