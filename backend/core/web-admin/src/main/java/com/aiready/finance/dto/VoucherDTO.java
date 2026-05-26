package com.aiready.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会计凭证DTO
 */
@Data
public class VoucherDTO {
    
    private Long id;
    
    /**
     * 凭证字号
     */
    private String voucherNo;
    
    /**
     * 凭证日期
     */
    private LocalDate voucherDate;
    
    /**
     * 会计期间
     */
    private String accountingPeriod;
    
    /**
     * 凭证类型（1：收款 2：付款 3：转账）
     */
    private Integer voucherType;
    
    /**
     * 凭证类型描述
     */
    private String voucherTypeDesc;
    
    /**
     * 凭证摘要
     */
    private String summary;
    
    /**
     * 借方金额合计
     */
    private BigDecimal totalDebit;
    
    /**
     * 贷方金额合计
     */
    private BigDecimal totalCredit;
    
    /**
     * 附件张数
     */
    private Integer attachmentCount;
    
    /**
     * 制单人ID
     */
    private Long creatorId;
    
    /**
     * 制单人姓名
     */
    private String creatorName;
    
    /**
     * 审核人ID
     */
    private Long reviewerId;
    
    /**
     * 审核人姓名
     */
    private String reviewerName;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 记账人ID
     */
    private Long bookkeeperId;
    
    /**
     * 记账人姓名
     */
    private String bookkeeperName;
    
    /**
     * 记账时间
     */
    private LocalDateTime bookkeepingTime;
    
    /**
     * 状态（0：草稿 1：待审核 2：已审核 3：已记账 4：已作废）
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 来源单据类型
     */
    private String sourceType;
    
    /**
     * 来源单据ID
     */
    private Long sourceId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 凭证分录列表
     */
    private List<VoucherItemDTO> items;
}
