package cn.aiedge.erp.sales.pricing.dto;

import cn.aiedge.erp.sales.pricing.entity.PriceApprovalRecord;
import cn.aiedge.erp.sales.pricing.entity.PriceSpecialApproval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 价格审批历史响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceApprovalResponseDTO {
    private PriceSpecialApproval approval;
    private List<PriceApprovalRecord> records;
    private int totalRecords;
}
