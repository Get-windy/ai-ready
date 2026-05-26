package com.aiready.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 会计凭证保存请求DTO
 */
@Data
public class VoucherSaveRequest {
    
    /**
     * 凭证日期
     */
    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;
    
    /**
     * 凭证类型（1：收款 2：付款 3：转账）
     */
    @NotNull(message = "凭证类型不能为空")
    private Integer voucherType;
    
    /**
     * 凭证摘要
     */
    @NotBlank(message = "凭证摘要不能为空")
    private String summary;
    
    /**
     * 附件张数
     */
    private Integer attachmentCount;
    
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
     * 凭证分录列表
     */
    @NotEmpty(message = "凭证分录不能为空")
    @Valid
    private List<VoucherItemSaveRequest> items;
}
