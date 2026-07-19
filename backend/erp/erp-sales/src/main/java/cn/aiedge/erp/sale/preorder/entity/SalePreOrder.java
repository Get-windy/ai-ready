package cn.aiedge.erp.sale.preorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预订货单实体
 */
@Data
@TableName("erp_sale_pre_order")
public class SalePreOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 单据编号 */
    private String orderNo;

    /** 客户ID */
    private Long customerId;
    /** 客户名称 */
    private String customerName;
    /** 客户编号 */
    private String customerCode;

    /** 开户行 */
    private String bankName;
    /** 银行账号 */
    private String bankAccount;
    /** 税号 */
    private String taxNo;

    /** 发货仓库ID */
    private Long warehouseId;
    /** 发货仓库名称 */
    private String warehouseName;

    /** 经手人ID */
    private Long handlerId;
    /** 经手人名称 */
    private String handlerName;

    /** 部门ID */
    private Long deptId;
    /** 部门名称 */
    private String deptName;

    /** 单据日期 */
    private LocalDate orderDate;

    /** 销售类型: 0=正常销售 1=样品销售 2=促销销售 */
    private Integer saleType;

    /** 收货人 */
    private String receiverName;
    /** 联系电话 */
    private String receiverPhone;
    /** 收货地址 */
    private String shippingAddress;

    /** 客户级别 */
    private String customerLevel;

    /** 客户一票通 */
    private String customerTicket;
    /** 客户备注 */
    private String customerRemark;

    /** 单据状态: 0=草稿 1=审核中 2=待订货 3=部分订货 4=已订货 5=已完成 -1=已取消 */
    private Integer status;

    /** 金额（商品总金额） */
    private BigDecimal totalAmount;
    /** 折后金额 */
    private BigDecimal discountedAmount;
    /** 本单金额 */
    private BigDecimal orderAmount;

    /** 结算状态: 0=未结算 1=部分结算 2=已结算 */
    private Integer settlementStatus;

    /** 已收预订金 */
    private BigDecimal receivedDeposit;
    /** 未收预订金 */
    private BigDecimal unreceivedDeposit;
    /** 本单预订金余额 */
    private BigDecimal depositBalance;

    /** 预订金账户1 */
    private String depositAccount1;
    /** 预订金账户2 */
    private String depositAccount2;
    /** 预订金账户3 */
    private String depositAccount3;
    /** 预订金账户4 */
    private String depositAccount4;

    /** 预订金金额 */
    private BigDecimal depositAmount;

    /** 信用额度 */
    private BigDecimal creditLimit;
    /** 收款期限 */
    private LocalDate depositDeadline;

    /** 预订数量 */
    private BigDecimal preOrderQuantity;
    /** 已订数量 */
    private BigDecimal orderedQuantity;
    /** 未订数量 */
    private BigDecimal unOrderedQuantity;
    /** 已发数量 */
    private BigDecimal shippedQuantity;
    /** 未发数量 */
    private BigDecimal unShippedQuantity;

    /** 重量(kg) */
    private BigDecimal totalWeight;
    /** 体积(m³) */
    private BigDecimal totalVolume;

    /** 区域 */
    private String region;

    /** 表尾自定义字段1 */
    private String footerExtText1;
    /** 表尾自定义字段2 */
    private String footerExtText2;

    /** 摘要 */
    private String summary;
    /** 单据备注 */
    private String remark;
    /** 附件 */
    private String attachment;

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

    /** 扩展信息(JSON) */
    private String extInfo;

    /** 制单人ID */
    private Long createBy;
    /** 制单人名称 */
    private String creatorName;
    /** 提交人ID */
    private Long submitBy;
    /** 提交人名称 */
    private String submitterName;
    /** 提交时间 */
    private LocalDateTime submitTime;
    /** 审核人ID */
    private Long approvedBy;
    /** 审核人名称 */
    private String auditorName;
    /** 审核时间 */
    private LocalDateTime approvedTime;

    /** 打印次数 */
    private Integer printCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 乐观锁版本号 */
    @Version
    private Integer versionNo;
}
