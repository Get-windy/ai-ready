package cn.aiedge.erp.purchase.saledriven.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 以销定购-查询参数
 * <p>
 * 以销定购页面本质是一个「销售订单视角的采购准备」查询，查询条件对齐对标 ql361 页面配置。
 * 数据源为 erp_sale_order 主表 + erp_sale_order_deposit 订金子表 + 采购单反查（是否已采购）。
 * 本模块在 erp-purchase 内跨库只读访问销售表，创建采购订单复用 {@code PurchaseOrderService}。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SalesDrivenQueryDTO {

    // ═══ 分页 ═══
    private Long current;
    private Long size;

    // ═══ 日期（单据日期） ═══
    private String dateStart;
    private String dateEnd;

    /** 单据编号 */
    private String orderNo;

    /** 销售类型（1正常/2样品/3促销） */
    private Integer saleType;

    /** 录入方式 */
    private String generationMethod;

    /** 客户 */
    private String customerName;

    /** 经手人 */
    private String salesmanName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String creatorName;

    /** 提交人 */
    private String submitterName;

    /** 审核人 */
    private String auditorName;

    /** 仓库 */
    private String warehouseName;

    /** 商品 */
    private String productName;

    /** 单据状态（0草稿/1待审核/2待发货/3部分发货/4发货完成/5交易完成/6已取消） */
    private Integer status;

    /** 记账状态（0未记账/1已记账） */
    private Integer bookkeepingStatus;

    /** 支付状态（0未支付/1部分支付/2已支付） */
    private Integer paymentStatus;

    /** 发货日期(起) */
    private String shipDateStart;

    /** 发货日期(止) */
    private String shipDateEnd;

    /** 收货人 */
    private String receiverName;

    /** 联系电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String shippingAddress;

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号 */
    private String waybillNo;

    /** 优惠券 */
    private BigDecimal couponAmount;

    /** 订金账户1 */
    private String depositAccount1;

    /** 订金账户2 */
    private String depositAccount2;

    /** 卖家备注 */
    private String sellerRemark;

    /** 买家备注 */
    private String buyerRemark;

    /** 只查看用了优惠券的订单 */
    private Boolean onlyCoupon;

    /** 显示已取消 */
    private Boolean showCancelled;

    /** 仅显示已选中（前端本地选中标记） */
    private Boolean onlySelected;
}
