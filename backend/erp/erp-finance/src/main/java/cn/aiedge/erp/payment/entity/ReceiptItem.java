package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_receipt_item")
public class ReceiptItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long receiptId;

    private Integer lineNo;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private BigDecimal orderAmount;

    private BigDecimal invoiceAmount;

    private BigDecimal pendingAmount;

    private BigDecimal verifyAmount;

    private BigDecimal verifiedAmount;

    private Integer verifyStatus;

    /** 结算单位编号 */
    private String settleUnitCode;

    /** 结算单位 */
    private String settleUnit;

    /** 往来单位 */
    private String tradeUnit;

    /** 结算单据编号 */
    private String settlementNo;

    /** 商品金额 */
    private BigDecimal productAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 费用/其他费用 */
    private BigDecimal otherFee;

    /** 运费 */
    private BigDecimal freight;

    /** 本单金额 */
    private BigDecimal billAmount;

    /** 已结金额 */
    private BigDecimal settledAmount;

    /** 未结金额 */
    private BigDecimal unsettledAmount;

    /** 本次优惠 */
    private BigDecimal currentDiscount;

    /** 本次结算 */
    private BigDecimal currentSettle;

    /** 源单经手人 */
    private String sourceHandlerName;

    /** 收货人 */
    private String receiverName;

    /** 联系电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 关联应收单ID(finance_receivable) */
    private Long receivableId;

    /** 核销来源类型: order/invoice/receivable */
    private String sourceType;

    private LocalDateTime verifiedTime;

    private Long verifiedBy;

    private String remark;

    @TableLogic
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