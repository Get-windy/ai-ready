package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产购置申请 DTO
 */
@Data
@Schema(description = "固定资产购置申请")
public class FixedAssetPurchaseDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "申请单号")
    private String purchaseNo;

    @Schema(description = "申请标题")
    private String title;

    @Schema(description = "申请人ID")
    private String applicantId;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "资产名称")
    private String assetName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "预估金额")
    private BigDecimal estimatedAmount;

    @Schema(description = "实际金额")
    private BigDecimal actualAmount;

    @Schema(description = "申请日期")
    private LocalDate applyDate;

    @Schema(description = "预计交付日期")
    private LocalDate expectedDeliveryDate;

    @Schema(description = "实际交付日期")
    private LocalDate actualDeliveryDate;

    @Schema(description = "购置原因")
    private String purchaseReason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "生成的资产ID")
    private Long generatedAssetId;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "供应商")
    private String supplierName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "更新人")
    private String updatedBy;
}
