package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 零售单主表实体
 * 遵循主从表快照模型：存外键ID + 快照字段（成交后锁定不变）
 * 预留 ext_num/ext_text/ext_partner/ext_staff/ext_dept 自定义扩展字段
 * <p>
 * 行业设计：B2B+B2C统一往来单位模型
 * - customer_id → biz_party.id（party_type=1, party_level='MEMBER' 或 'ENTERPRISE'）
 * - 默认散客：系统预置的 WALKIN 会员（biz_party.party_code='WALKIN'）
 * - 有会员时：customer_id 指向真实的个人会员 party
 */
@Data
@Accessors(chain = true)
@TableName("erp_retail_order")
public class RetailOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    // ═══ 单据基本信息 ═══
    private String retailNo;             // 零售单号（LSD-YYYYMMDD-NNNN）
    private LocalDate orderDate;         // 单据日期
    private String saleType;             // 销售类型（NORMAL/RETURN）
    private Integer status;              // 状态：0=草稿 1=已完成 2=挂单 3=已作废
    private String generationMethod;     // 产生方式（手工/订单生成/复制/POS）
    private String summary;              // 摘要
    private String sourceBillNo;         // 来源单据编号

    // ═══ 客户快照 ═══
    private Long customerId;             // 客户ID → biz_party.id
    private String customerName;         // 客户名称（快照）
    private String customerCode;         // 客户编号（快照）
    private String customerLevel;        // 客户级别（快照）
    private Long contactId;              // 联系人ID
    private String contactName;          // 联系人姓名
    private String customerRemark;       // 客户备注（快照）

    // ═══ 银行/税务 ═══
    private String bankName;             // 开户银行
    private String bankAccount;          // 银行账号
    private String taxNo;                // 税号

    // ═══ 仓库/经手人/组织 ═══
    private Long warehouseId;
    private String warehouseName;
    private Long handlerId;              // 经手人ID
    private String handlerName;          // 经手人姓名
    private Long departmentId;           // 部门ID
    private String departmentName;       // 部门名称
    private String region;               // 区域
    private String location;             // 点位

    // ═══ 会员信息 ═══
    private String memberCardNo;         // 会员卡号
    private String memberName;           // 会员姓名

    // ═══ 数量/金额 ═══
    private BigDecimal totalQuantity;    // 总数量
    private BigDecimal amount;           // 商品总金额（优惠前）
    private BigDecimal directDiscount;   // 直接优惠（手动折扣）
    private BigDecimal couponDiscount;   // 优惠券抵扣
    private BigDecimal promoDiscount;    // 促销优惠（活动折扣）
    private BigDecimal otherFee;         // 其他费用
    private BigDecimal roundingAmount;   // 抹零金额
    private BigDecimal totalWeight;      // 总重量(kg)
    private BigDecimal totalVolume;      // 总体积(m³)
    private BigDecimal returnQuantity;   // 退货数量
    private BigDecimal returnAmount;     // 退货金额
    private Integer boxCount;            // 商品行数/箱数

    // ═══ 应收/应付 ═══
    private BigDecimal payableAmount;    // 应付金额

    // ═══ 预收款 ═══
    private BigDecimal advancePaymentAmount;    // 使用预收款金额
    private BigDecimal prevAdvancePayment;      // 此前预收
    private BigDecimal usedAdvancePayment;      // 使用预收款
    private BigDecimal availableAdvancePayment; // 可用预收
    private BigDecimal prepaidBalance;          // 预收余额

    // ═══ 信用 ═══
    private BigDecimal creditLimit;             // 信用额度
    private BigDecimal availableCredit;         // 可用额度
    private BigDecimal prevArrears;             // 此前欠款
    private BigDecimal currentArrears;          // 本次欠款
    private BigDecimal arrearsBalance;          // 欠款余额

    // ═══ 积分 ═══
    private Integer prevPoints;                 // 此前积分
    private BigDecimal memberGeneratedPoints;   // 产生积分
    private BigDecimal memberUsedPoints;        // 使用积分
    private BigDecimal memberExchangePoints;    // 兑换积分
    private BigDecimal currentPoints;           // 当前积分

    // ═══ 收款方式（汇总字段） ═══
    private String paymentMethod;              // 支付方式：CASH/ALIPAY/WECHAT/CARD/PREPAID/TRANSFER/MIXED
    private BigDecimal cashAmount;             // 现金支付
    private BigDecimal cardAmount;             // 银行卡支付
    private BigDecimal alipayAmount;           // 支付宝支付
    private BigDecimal wechatAmount;           // 微信支付
    private BigDecimal aggregateAmount;        // 聚合支付
    private BigDecimal abcAmount;              // 中国农业银行支付
    private BigDecimal ccbAmount;              // 中国建设银行支付
    private BigDecimal jdAmount;               // 京东支付
    private BigDecimal prepaidAmount;          // 预收款抵扣
    private BigDecimal transferAmount;         // 转账支付
    private BigDecimal totalReceived;          // 收款合计
    private Boolean combinedPayment;           // 是否组合支付
    private BigDecimal changeAmount;           // 找零

    // ═══ 收款账户（4个） ═══
    private String paymentAccount1;
    private String paymentAccount2;
    private String paymentAccount3;
    private String paymentAccount4;

    // ═══ 收款码 ═══
    private String paymentQrCode;              // 收款码

    // ═══ 收银员 ═══
    private Long cashierId;                    // 收银员ID
    private String cashierName;                // 收银员姓名

    // ═══ 结算 ═══
    private String settlementMethod;           // 结算方式
    private BigDecimal settledAmount;          // 结算金额
    private String settlementStatus;           // 结算状态：UNSETTLED/SETTLED/PARTIAL

    // ═══ 流程时间/人员 ═══
    private String bookkeeperName;             // 记账人
    private String creatorName;                // 制单人
    private String auditorName;                // 审核人
    private Integer printCount;                // 打印次数
    private LocalDateTime bookkeepingTime;     // 记账时间
    private LocalDateTime printTime;           // 打印时间
    private Long approvedBy;                   // 审核人ID
    private LocalDateTime approvedTime;        // 审核时间
    private Long completedBy;                  // 完成人ID
    private LocalDateTime completedTime;       // 完成时间

    // ═══ 附件 ═══
    private String attachment;                 // 附件URL或JSON数组

    // ═══ 计算字段（非持久化） ═══
    @TableField(exist = false)
    private BigDecimal discountedAmount;       // 折后金额

    @TableField(exist = false)
    private BigDecimal favorableAmount;        // 优惠后金额

    @TableField(exist = false)
    private BigDecimal totalDiscount;          // 优惠金额总计

    // ═══ 备注 ═══
    private String remark;                     // 单据备注
    private String internalNote;               // 内部备注
    private String buyerRemark;                // 买家备注

    // ═══ 表头自定义字段（数字 1-5） ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;

    // ═══ 表头自定义字段（文本 1-5） ═══
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;

    // ═══ 表头自定义字段（往来单位/职员/部门） ═══
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ═══ 表尾自定义字段 ═══
    private String footerExtText1;
    private String footerExtText2;

    // ═══ POS专用 ═══
    private Boolean posMode;                   // POS模式
    private String posSessionId;               // POS班次ID
    private String posTerminal;                // POS终端
    private Boolean holdOrderFlag;             // 挂单标记

    // ═══ 系统字段 ═══
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

    @Version
    private Integer versionNo;
}
