package cn.aiedge.erp.stock.controller.initial;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 期初库存数据传输对象（新增/编辑入参）
 * <p>
 * ⚠️ 历史缺陷（2026-09-18 修复）：原 DTO 只有 productCode/productName/warehouseName，
 * **没有 productId / warehouseId** → 弹窗里选的商品与仓库根本传不出去，数据无法归属到具体商品/仓库。
 * 现补齐 productId / warehouseId，名称类字段仍作为冗余快照保留（列表展示不必回查商品档案）。
 * <p>
 * 同时补齐 unit / productionDate / validityDate / remark：
 * 原 DTO 缺这四项，弹窗填了也不落库（erp_stock 表本身有 unit / production_date / validity_date / remark 列）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InitialStockDTO {

    /** 期初行主键（编辑时必填） */
    private Long id;

    /** 商品ID（新增/编辑必填） */
    private Long productId;

    /** 商品名称（快照） */
    private String productName;

    /** 货号（快照，对应 erp_stock.product_code） */
    private String productCode;

    /** 仓库ID（新增/编辑必填） */
    private Long warehouseId;

    /** 仓库名称（快照） */
    private String warehouseName;

    /** 小单位（快照，写入 erp_stock.unit） */
    private String unit;

    /** 期初数量 */
    private BigDecimal quantity;

    /** 期初成本单价 */
    private BigDecimal unitPrice;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 有效期至（与 DB 列 validity_date 同名口径） */
    private LocalDateTime validityDate;

    /** 备注 */
    private String remark;
}
