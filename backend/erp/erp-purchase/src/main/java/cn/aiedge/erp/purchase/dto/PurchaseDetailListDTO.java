package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 按明细Tab列表返回DTO（59列）
 * 包含单据级字段+明细级字段拼接
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDetailListDTO {

    // ═══ 单据级字段(1-12) ═══
    /** 1 单据日期 */
    private LocalDateTime orderDate;
    /** 2 单据编号 */
    private String orderNo;
    /** 3 单据状态 */
    private Integer status;
    /** 4 仓库 */
    private String warehouseName;
    /** 5 供应商名称 */
    private String supplierName;
    /** 6 供应商编号 */
    private String supplierCode;
    /** 7 联系人 */
    private String contactName;
    /** 8 联系电话 */
    private String contactPhone;
    /** 9 联系地址 */
    private String contactAddress;
    /** 10 供应商备注 */
    private String supplierRemark;
    /** 11 经手人 */
    private String purchaserName;
    /** 12 部门 */
    private String deptName;

    // ═══ 明细级字段(13-59) ═══
    /** 13 商品名称 */
    private String productName;
    /** 14 货号 */
    private String itemCode;
    /** 15 条码 */
    private String barcode;
    /** 16 规格 */
    private String specification;
    /** 17 型号 */
    private String model;
    /** 18 产地 */
    private String origin;
    /** 19 品牌 */
    private String brand;
    /** 20 表体自定义1(数字) */
    private BigDecimal customField1;
    /** 21 表体自定义2(数字) */
    private BigDecimal customField2;
    /** 22 表体自定义3(数字) */
    private BigDecimal customField3;
    /** 23 表体自定义4(文本) */
    private String customField4;
    /** 24 表体自定义5(文本) */
    private String customField5;
    /** 25 表体自定义6(数字) */
    private BigDecimal customField6;
    /** 26 表体自定义7(数字) */
    private BigDecimal customField7;
    /** 27 表体自定义8(往来单位) */
    private Long customField8;
    /** 28 表体自定义9(职员) */
    private Long customField9;
    /** 29 表体自定义10(部门) */
    private Long customField10;
    /** 30 单位 */
    private String unit;
    /** 31 小单位 */
    private String smallUnit;
    /** 32 小单位数量 */
    private BigDecimal smallUnitQuantity;
    /** 33 换算关系 */
    private String conversionRelation;
    /** 34 换算结果 */
    private BigDecimal convertedQuantity;
    /** 35 大包装 */
    private BigDecimal bigPack;
    /** 36 中包装 */
    private BigDecimal midPack;
    /** 37 小包装 */
    private BigDecimal smallPack;
    /** 38 订货数量 */
    private BigDecimal quantity;
    /** 39 已收数量 */
    private BigDecimal receivedQuantity;
    /** 40 未收数量 */
    private BigDecimal unreceiveQuantity;
    /** 41 终止数量 */
    private BigDecimal terminatedQuantity;
    /** 42 终止金额 */
    private BigDecimal terminatedAmount;
    /** 43 单价 */
    private BigDecimal unitPrice;
    /** 44 小单位单价 */
    private BigDecimal smallUnitPrice;
    /** 45 金额 */
    private BigDecimal amount;
    /** 46 优惠折扣(%) */
    private BigDecimal discountRate;
    /** 47 优惠后单价 */
    private BigDecimal discountedUnitPrice;
    /** 48 优惠后金额 */
    private BigDecimal discountedAmount;
    /** 49 重量(kg) */
    private BigDecimal weight;
    /** 50 体积(m³) */
    private BigDecimal volume;
    /** 51 明细备注 */
    private String itemRemark;
    /** 52 单据备注 */
    private String remark;
    /** 53 摘要 */
    private String summary;
    /** 54 附件 */
    private String attachment;
    /** 55 制单人 */
    private String createByName;
    /** 56 审核人 */
    private String auditorName;
    /** 57 制单时间 */
    private LocalDateTime createTime;
    /** 58 提交时间 */
    private LocalDateTime submitTime;
    /** 59 打印次数 */
    private Integer printCount;

    /** 明细ID */
    private Long itemId;
    /** 主表ID */
    private Long orderId;
}
