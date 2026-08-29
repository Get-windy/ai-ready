package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购费用分摊创建请求DTO
 *
 * 对应表单页：主表10字段 + 费用单明细表9列 + 采购入库单分摊明细表12列。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CostSharingCreateRequest {

    /** 经手人ID */
    private Long handlerId;

    /** 经手人名称 */
    private String handlerName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 单据日期 */
    private LocalDate sharingDate;

    /** 分摊方式（amount/quantity/weight） */
    private String sharingMethod;

    /** 摘要 */
    private String summary;

    /** 单据备注 */
    private String remark;

    /** 制单人名称 */
    private String createByName;

    /** 费用单明细列表 */
    private List<ExpenseItemDTO> expenseItems;

    /** 采购入库单分摊明细 */
    private List<AllocationDetailDTO> details;

    @Data
    public static class ExpenseItemDTO {
        /** 费用单编号 */
        private String expenseNo;
        /** 往来单位ID */
        private Long partnerId;
        /** 往来单位名称 */
        private String partnerName;
        /** 往来单位编码 */
        private String partnerCode;
        /** 结算单位编号 */
        private String settleUnitId;
        /** 结算单位 */
        private String settleUnit;
        /** 费用项 */
        private String expenseType;
        /** 费用金额 */
        private BigDecimal expenseAmount;
        /** 备注 */
        private String remark;
    }

    @Data
    public static class AllocationDetailDTO {
        /** 入库单ID */
        private Long inboundId;
        /** 入库单编号 */
        private String inboundNo;
        /** 供应商ID */
        private Long supplierId;
        /** 供应商名称 */
        private String supplierName;
        /** 供应商编码 */
        private String supplierCode;
        /** 结算单位编号 */
        private String settleUnitId;
        /** 结算单位 */
        private String settleUnit;
        /** 商品ID */
        private Long productId;
        /** 商品名称 */
        private String productName;
        /** 计价单位 */
        private String pricingUnit;
        /** 数量 */
        private BigDecimal quantity;
        /** 优惠后单价 */
        private BigDecimal discountedUnitPrice;
        /** 优惠后金额 */
        private BigDecimal discountedAmount;
        /** 分摊金额 */
        private BigDecimal allocatedCost;
    }
}
