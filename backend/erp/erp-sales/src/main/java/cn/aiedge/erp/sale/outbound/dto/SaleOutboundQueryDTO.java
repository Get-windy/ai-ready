package cn.aiedge.erp.sale.outbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售出库单多条件查询参数
 * <p>查询项与对标文档「按单据 40 项查询条件」一一对应；按明细 Tab 额外使用
 * {@link #productName} / {@link #itemRemark} / {@link #gift} / {@link #promoProduct} / {@link #productAttribute}。</p>
 */
@Data
public class SaleOutboundQueryDTO {

    // ═══ 分页 ═══
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 通用关键词（单据编号 / 来源订单 / 客户名 模糊） */
    private String keyword;

    // ═══ 日期 ═══
    private String dateStart;
    private String dateEnd;

    // ═══ 单据 ═══
    private String outboundNo;
    private Integer status;
    private String sourceOrder;
    private String generationMethod;
    private Integer outboundType;
    private Integer printCount;
    private BigDecimal totalAmount;

    // ═══ 查询方案/归类 ═══
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private Long salesPersonId;
    private String salesPersonName;
    private String departmentName;
    private String region;

    // ═══ 结算 ═══
    private String settlementStatus;
    private String settlementMethod;

    // ═══ 备注/摘要 ═══
    private String remark;
    private String buyerRemark;
    private String summary;

    // ═══ 人员 ═══
    private String creatorName;
    private String auditorName;
    private String bookkeeperName;

    // ═══ 表头自定义字段 ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;

    // ═══ 收货 ═══
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    // ═══ 物流 ═══
    private String logisticsCompany;
    private String trackingNumber;
    private String deliveryMethod;
    private String deliveryDriver;
    private String logisticsRemark;

    // ═══ 收款账户 ═══
    private String paymentAccount1;
    private String paymentAccount2;
    private String paymentAccount3;
    private String paymentAccount4;

    // ═══ 勾选项 ═══
    /** 显示红冲：默认 false，过滤掉红字（负数金额）单据 */
    private Boolean showRed;
    /** 仅显示异常记账单据：已完成但凭证未生成（记账时间为空） */
    private Boolean showAbnormal;

    // ═══ 按明细 Tab 专用 ═══
    private Long productId;
    private String productName;
    private String itemRemark;
    private Boolean gift;
    /** 促销商品：明细存在优惠折扣的商品行 */
    private Boolean promoProduct;
    private String productAttribute;

    // ═══ 商品分类树 ═══
    private String categoryId;

    // ═══ 发货查询（配发收 → 发货查询）固定项 ═══
    /**
     * 配送执行命中的出库单号集合。
     *
     * <p>「配送状态 / 配送线路」不是出库单自身的列，而是配送任务（`dms_task`）的执行属性；
     * 由 DMS 侧 `GET /api/dms/task/outbound-filter` 依据来源单据号反查出单号集合后，
     * 本页以本字段做精确过滤——跨域只走接口，不在销售模块内 JOIN DMS 表结构。</p>
     */
    private List<String> outboundNos;
}
