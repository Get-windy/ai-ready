package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购费用分摊创建请求DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CostSharingCreateRequest {

    /** 费用项列表 */
    private List<ExpenseItemDTO> expenseItems;

    /** 入库单ID列表 */
    private List<Long> inboundIds;

    /** 分摊方式（amount/quantity/weight） */
    private String sharingMethod;

    /** 分摊明细 */
    private List<AllocationDetailDTO> details;

    @Data
    public static class ExpenseItemDTO {
        private String expenseType;
        private BigDecimal amount;
        private String source;
        private Long sourceDocId;
        private String remark;
    }

    @Data
    public static class AllocationDetailDTO {
        private Long inboundId;
        private String inboundNo;
        private String productName;
        private String productCode;
        private BigDecimal quantity;
        private BigDecimal amount;
        private BigDecimal baseValue;
        private String shareRatio;
        private BigDecimal sharedAmount;
    }
}
