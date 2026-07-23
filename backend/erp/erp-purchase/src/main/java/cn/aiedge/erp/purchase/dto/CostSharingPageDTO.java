package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购费用分摊列表查询结果DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CostSharingPageDTO {

    private Long id;
    private String sharingNo;
    private LocalDateTime sharingDate;
    private String supplierName;
    private String expenseType;
    private String allocationMethod;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createByName;
}
