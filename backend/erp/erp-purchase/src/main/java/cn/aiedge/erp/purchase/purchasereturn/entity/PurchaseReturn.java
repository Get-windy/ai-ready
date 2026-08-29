package cn.aiedge.erp.purchase.purchasereturn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_purchase_return")
public class PurchaseReturn {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 单号编号 */
    private String returnNo;

    /** 源采购订单ID */
    private Long purchaseOrderId;

    /** 源采购订单号 */
    private String purchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    /** 供应商编号 */
    private String supplierNo;

    /** 开户行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 税号 */
    private String taxNo;

    /** 出库仓库 */
    private Long warehouseId;

    private String warehouseName;

    /** 经手人 */
    private Long purchaserId;

    private String purchaserName;

    /** 部门 */
    private Long departmentId;

    private String departmentName;

    /** 单据日期 */
    private LocalDate returnDate;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 联系地址 */
    private String contactAddress;

    /** 供应商备注 */
    private String supplierRemark;

    private Integer returnType;

    private String returnTypeDesc;

    /** 退货数量 */
    private BigDecimal totalQuantity;

    /** 金额 */
    private BigDecimal totalAmount;

    /** 折后金额 */
    private BigDecimal discountAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 本单金额 */
    private BigDecimal totalAmountWithTax;

    /** 已结金额 */
    private BigDecimal settledAmount;

    /** 结算状态：0未结算 1已结算 */
    private Integer settleStatus;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 体积(m³) */
    private BigDecimal volume;

    /** 摘要 */
    private String summary;

    /** 表头自定义字段1(数字) */
    private BigDecimal extNum1;

    /** 表头自定义字段2(数字) */
    private BigDecimal extNum2;

    /** 表头自定义字段3(文本) */
    private String extText1;

    /** 表头自定义字段4(文本) */
    private String extText2;

    /** 表头自定义字段5(文本) */
    private String extText3;

    // ── 收款区 ──
    /** 收款账户 */
    private String paymentAccount;

    /** 收款金额 */
    private BigDecimal paymentAmount;

    /** 更多账户 */
    private String moreAccounts;

    /** 此前预付 */
    private BigDecimal prevPrepaid;

    /** 退回预付款 */
    private BigDecimal refundPrepay;

    /** 预付余额 */
    private BigDecimal prepaidBalance;

    /** 本次欠款 */
    private BigDecimal currentDebt;

    /** 此前欠款 */
    private BigDecimal prevDebt;

    /** 欠款余额 */
    private BigDecimal debtBalance;

    /** 付款期限 */
    private LocalDate paymentDeadline;

    /** 单据备注 */
    private String remark;

    /** 制单人 */
    private String createByName;

    /** 记账人 */
    private String posterName;

    /** 记账时间 */
    private LocalDateTime postTime;

    /** 附件 */
    private String attachment;

    /** 打印次数 */
    private Integer printCount;

    private Integer status;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applyTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private String reason;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}
