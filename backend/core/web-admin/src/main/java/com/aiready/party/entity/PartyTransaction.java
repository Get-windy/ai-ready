package com.aiready.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 往来单位交易记录实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("biz_party_transaction")
public class PartyTransaction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 单位ID
     */
    private Long partyId;
    
    /**
     * 交易日期
     */
    private LocalDate transactionDate;
    
    /**
     * 交易类型（1：销售 2：采购 3：收款 4：付款 5：退款 6：其他）
     */
    private Integer transactionType;
    
    /**
     * 单据类型（ORDER/SALE/PURCHASE/RECEIPT/PAYMENT等）
     */
    private String documentType;
    
    /**
     * 单据ID
     */
    private Long documentId;
    
    /**
     * 单据编号
     */
    private String documentNo;
    
    /**
     * 交易金额（正数表示应收/收入，负数表示应付/支出）
     */
    private BigDecimal amount;
    
    /**
     * 应收金额
     */
    private BigDecimal receivableAmount;
    
    /**
     * 应付金额
     */
    private BigDecimal payableAmount;
    
    /**
     * 已收金额
     */
    private BigDecimal receivedAmount;
    
    /**
     * 已付金额
     */
    private BigDecimal paidAmount;
    
    /**
     * 余额（累计欠款）
     */
    private BigDecimal balance;
    
    /**
     * 摘要/说明
     */
    private String summary;
    
    /**
     * 经手人ID
     */
    private Long operatorId;
    
    /**
     * 状态（0：作废 1：正常）
     */
    private Integer status;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
