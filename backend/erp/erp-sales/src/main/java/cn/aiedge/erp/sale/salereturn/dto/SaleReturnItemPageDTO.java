package cn.aiedge.erp.sale.salereturn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售退货申请「按明细」分页行（{@code GET /erp/sale/return/page-detail}）。
 *
 * <p>数据源：{@code erp_sale_return} (r) LEFT JOIN {@code erp_sale_return_item} (i)。
 * 一行 = 一条退货申请明细，单头字段随行冗余。两个页面共用本端点，字段名保持向后兼容：</p>
 * <ul>
 *   <li>「商城 → 订单处理 → 退货申请处理」按明细 Tab（{@code views/mall/return-process}）</li>
 *   <li>「销售 → 退货申请」按明细 Tab（{@code views/sales/return-apply}）</li>
 * </ul>
 *
 * <p><b>字段口径（列必须真实存在）</b>：全部取自已生效建表
 * {@code V8.99.9__Create_Sale_Return_Apply_Tables.sql} 及其后的 ALTER 迁移
 * （V9.38.0 / V9.39.0 / V9.53.0 / V11.100.0 / V11.311.0），未新增任何列。</p>
 *
 * <ul>
 *   <li><b>单头展示列</b>：orderDate / returnNo / sourceOrder / status / returnType / settleStatus /
 *       warehouseName / customerName / handlerName / remark / approvedNote（审核备注）/
 *       submitTime / createTime / auditTime / printCount …</li>
 *   <li><b>明细展示列</b>：productName / productCode / barcode / specification / modelNo / originPlace /
 *       brand / unit / returnQuantity / receivedQuantity / unitPrice / lineAmount / reason / itemRemark …</li>
 *   <li><b>表头自定义字段（单头 r.ext_*）</b>：headerExtNum1、headerExtNum2、headerExtText3、
 *       headerExtText4、headerExtText5</li>
 *   <li><b>单据自定义字段（明细 i.ext_*）</b>：extNum1~5、extText1~2
 *       ——命名沿用 sales/return-apply 既有口径，避免破坏该页按明细列取值；
 *       本页新增的表头自定义字段一律走 {@code headerExt*} 前缀，二者不冲突。</li>
 * </ul>
 *
 * <p><b>派生列</b>：</p>
 * <ul>
 *   <li>{@code unreceivedQuantity} = 退货数量 − COALESCE(已收数量, 0)（明细表无"未收数量"列）</li>
 *   <li>{@code orderedQuantity} = 明细申请退货数量（本表无独立"订货数量"列；主表
 *       {@code ordered_quantity} 由 {@code calculateTotals} 写入的正是"明细退货数量合计"，
 *       故明细行取 {@code i.return_quantity}，与主表口径一致）</li>
 *   <li>{@code approvedNote} 为「审核备注」列的真实来源（{@code approve(id, note)} 写 note、
 *       {@code reject(id, reason)} 写拒绝原因，同一列），非新增列</li>
 * </ul>
 *
 * <p>库中确实无对应列的展示项（如 {@code audit_remark}）不在此产出，前端对应列保持空值，
 * 不做假数据填充。</p>
 */
@Data
public class SaleReturnItemPageDTO {

    // ==================== 行标识 ====================

    /** 明细行主键（前端行键使用 itemId，避免与单头 id 一对多导致 :key 重复） */
    private Long itemId;

    /** 退货申请单主键 */
    private Long id;

    /** 行号 */
    private Integer lineNo;

    // ==================== 单头（erp_sale_return） ====================

    /** 单据编号 */
    private String returnNo;

    /** 单据日期 */
    private LocalDateTime orderDate;

    /** 来源订单/来源单据（r.source_order） */
    private String sourceOrder;

    /** 单据状态：0草稿 1待审批 2已审批 3已完成 4已取消 */
    private Integer status;

    /** 退货类型（枚举，库中无字典表，前端原样展示） */
    private Integer returnType;

    /** 结算状态 */
    private String settleStatus;

    /** 销售类型 */
    private String salesType;

    /** 产生方式 */
    private String generateType;

    /** 打印次数 */
    private Integer printCount;

    /** 仓库名称快照 */
    private String warehouseName;

    /** 客户名称快照（本表无独立"结算单位"列，结算单位即客户快照） */
    private String customerName;

    /** 客户编号快照 */
    private String customerCode;

    /** 客户级别快照 */
    private String customerLevel;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 联系地址 */
    private String contactAddress;

    /** 客户一票通 */
    private String customerTicket;

    /** 客户备注 */
    private String customerRemark;

    /** 经手人姓名快照 */
    private String handlerName;

    /** 部门名称快照 */
    private String deptName;

    /** 单据备注 */
    private String remark;

    /** 审核备注（真实来源 = approved_note：审批意见/拒绝原因，非新增列） */
    private String approvedNote;

    /** 摘要 */
    private String summary;

    /** 附件 */
    private String attachment;

    /** 制单人 */
    private String creatorName;

    /** 审核人 */
    private String auditorName;

    /** 提交人ID（列表"提交人"列由 submit_by 解析） */
    private Long submitBy;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 制单日期 */
    private LocalDateTime createTime;

    /** 退货原因（单头 r.reason） */
    private String reason;

    /** 退货数量合计（单头） */
    private BigDecimal totalQuantity;

    /** 退货金额合计（单头） */
    private BigDecimal totalAmount;

    // ── 物流（单头，保留原有产出，供其他消费方使用） ──
    private String deliveryMethod;
    private String deliveryNo;
    private String waybillNo;
    private String logisticsCompany;
    private BigDecimal shippingFee;
    private String freightPayer;

    /** 重量合计 */
    private BigDecimal totalWeight;

    /** 体积合计 */
    private BigDecimal totalVolume;

    // ==================== 表头自定义字段（单头 r.ext_*） ====================

    /** 表头自定义字段1（数字） */
    private BigDecimal headerExtNum1;

    /** 表头自定义字段2（数字） */
    private BigDecimal headerExtNum2;

    /** 表头自定义字段3（文本） */
    private String headerExtText3;

    /** 表头自定义字段4（文本） */
    private String headerExtText4;

    /** 表头自定义字段5（文本） */
    private String headerExtText5;

    // ==================== 明细行（erp_sale_return_item） ====================

    /** 商品名称 */
    private String productName;

    /** 商品货号 */
    private String productCode;

    /** 条码 */
    private String barcode;

    /** 规格 */
    private String specification;

    /** 型号 */
    private String modelNo;

    /** 产地 */
    private String originPlace;

    /** 品牌 */
    private String brand;

    /** 产品图片 */
    private String imageUrl;

    /** 单位 */
    private String unit;

    /** 小单位 */
    private String smallUnit;

    /** 小单位数量 */
    private BigDecimal smallUnitQuantity;

    /** 换算关系 */
    private String conversionRelation;

    /** 换算结果 */
    private BigDecimal conversionResult;

    /** 大包装 */
    private BigDecimal bigPack;

    /** 中包装 */
    private BigDecimal midPack;

    /** 小包装 */
    private BigDecimal smallPack;

    /** 件数 */
    private BigDecimal pieceQuantity;

    /** 订货数量（派生：取明细申请退货数量，见类注释） */
    private BigDecimal orderedQuantity;

    /** 退货数量（明细） */
    private BigDecimal returnQuantity;

    /** 已收数量（明细，由收货回写 writebackReceivedProgress 落库） */
    private BigDecimal receivedQuantity;

    /** 未收数量（派生：退货数量 − 已收数量） */
    private BigDecimal unreceivedQuantity;

    /** 终止数量 */
    private BigDecimal terminatedQuantity;

    /** 终止金额 */
    private BigDecimal terminatedAmount;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 退货金额/行金额（前端按明细「退货金额」列 key = lineAmount） */
    private BigDecimal lineAmount;

    /** 小单位单价 */
    private BigDecimal smallUnitPrice;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 折后单价 */
    private BigDecimal discountedPrice;

    /** 折后金额 */
    private BigDecimal discountedAmount;

    /** 参考成本单价 */
    private BigDecimal refCostPrice;

    /** 参考成本金额 */
    private BigDecimal refCostAmount;

    /** 重量 */
    private BigDecimal weight;

    /** 体积 */
    private BigDecimal volume;

    /** 是否赠品 */
    private Boolean isGift;

    /** 商品行属性 */
    private String productLineAttr;

    /** 明细备注（前端按明细「明细备注」列 key = itemRemark / 查询项"备注"） */
    private String itemRemark;

    /** 兑换礼品 */
    private String exchangeGift;

    /** 兑换积分 */
    private BigDecimal exchangePoints;

    // ── 价格等级（8 个标准产品价格等级） ──
    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;

    // ── 单据自定义字段（明细 i.ext_*，命名沿用 sales/return-apply 口径） ──
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private String extText1;
    private String extText2;
    private Long extPartner;
    private Long extStaff;
    private Long extDept;
}
