package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发票实体类
 * 支持采购发票和销售发票
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_invoice")
public class Invoice {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 发票号码
     */
    private String invoiceNo;
    
    /**
     * 发票类型：1-增值税专用发票 2-增值税普通发票 3-电子发票 4-机动车发票 5-其他
     */
    private Integer invoiceType;
    
    /**
     * 发票方向：1-采购发票 2-销售发票
     */
    private Integer direction;
    
    /**
     * 发票状态：0-草稿 1-待审核 2-已审核 3-已开具 4-已作废 5-已红冲 6-已归档
     */
    private Integer status;
    
    /**
     * 关联业务类型：1-采购订单 2-销售订单 3-费用报销
     */
    private Integer bizType;
    
    /**
     * 关联业务ID
     */
    private Long bizId;
    
    /**
     * 关联业务单号
     */
    private String bizNo;
    
    /**
     * 开票日期
     */
    private LocalDate invoiceDate;
    
    /**
     * 所属期间（税务申报用）
     */
    private String taxPeriod;
    
    /**
     * 销方名称
     */
    private String sellerName;
    
    /**
     * 销方税号
     */
    private String sellerTaxNo;
    
    /**
     * 销方地址电话
     */
    private String sellerAddressPhone;
    
    /**
     * 销方银行账号
     */
    private String sellerBankAccount;
    
    /**
     * 购方名称
     */
    private String buyerName;
    
    /**
     * 购方税号
     */
    private String buyerTaxNo;
    
    /**
     * 购方地址电话
     */
    private String buyerAddressPhone;
    
    /**
     * 购方银行账号
     */
    private String buyerBankAccount;
    
    /**
     * 金额（不含税）
     */
    private BigDecimal amount;
    
    /**
     * 税率
     */
    private BigDecimal taxRate;
    
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    
    /**
     * 价税合计
     */
    private BigDecimal totalAmount;
    
    /**
     * 已核销金额
     */
    private BigDecimal verifiedAmount;
    
    /**
     * 未核销金额
     */
    private BigDecimal unverifiedAmount;
    
    /**
     * 币种
     */
    private String currency;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 开票人
     */
    private String drawer;
    
    /**
     * 复核人
     */
    private String reviewer;
    
    /**
     * 收款人
     */
    private String payee;
    
    /**
     * 是否认证：0-未认证 1-已认证 2-认证失败
     */
    private Integer certified;
    
    /**
     * 认证日期
     */
    private LocalDate certifyDate;
    
    /**
     * 认证方式：1-扫描认证 2-勾选认证 3-自动认证
     */
    private Integer certifyMethod;
    
    /**
     * 是否打印：0-未打印 1-已打印
     */
    private Integer printed;
    
    /**
     * 打印次数
     */
    private Integer printCount;
    
    /**
     * 是否电子发票：0-纸质 1-电子
     */
    private Integer electronic;
    
    /**
     * 电子发票下载地址
     */
    private String electronicUrl;
    
    /**
     * 作废原因
     */
    private String voidReason;
    
    /**
     * 红冲原因
     */
    private String redReason;
    
    /**
     * 原发票ID（红冲用）
     */
    private Long originalInvoiceId;
    
    /**
     * 原发票号码（红冲用）
     */
    private String originalInvoiceNo;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    private Integer version;
}
