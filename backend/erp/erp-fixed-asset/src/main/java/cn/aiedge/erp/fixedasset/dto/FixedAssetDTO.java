package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产DTO
 */
@Data
@Schema(description = "固定资产")
public class FixedAssetDTO {

    @Schema(description = "ID")
    private Long id;

    @NotBlank(message = "资产编码不能为空")
    @Size(max = 50, message = "资产编码长度不能超过50")
    @Schema(description = "资产编码")
    private String assetCode;

    @NotBlank(message = "资产名称不能为空")
    @Size(max = 200, message = "资产名称长度不能超过200")
    @Schema(description = "资产名称")
    private String assetName;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @NotNull(message = "购入日期不能为空")
    @Schema(description = "购入日期")
    private LocalDate purchaseDate;

    @NotNull(message = "资产原值不能为空")
    @Positive(message = "资产原值必须大于0")
    @Schema(description = "资产原值")
    private BigDecimal originalValue;

    @Schema(description = "净值")
    private BigDecimal netValue;

    @Schema(description = "折旧方法")
    private String depreciationMethod;

    @Schema(description = "使用年限（月）")
    private Integer usefulLife;

    @Schema(description = "残值")
    private BigDecimal salvageValue;

    @Schema(description = "残值率")
    private BigDecimal salvageRate;

    @Schema(description = "月折旧额")
    private BigDecimal monthlyDepreciation;

    @Schema(description = "累计折旧")
    private BigDecimal accumulatedDepreciation;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "存放地点")
    private String location;

    @Schema(description = "部门ID")
    private String departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "保管人ID")
    private String custodianId;

    @Schema(description = "保管人姓名")
    private String custodianName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "供应商")
    private String supplierName;

    @Schema(description = "发票号")
    private String invoiceNo;

    @Schema(description = "保修截止日期")
    private LocalDate warrantyEndDate;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;

    @Schema(description = "使用状态")
    private String useStatus;

    @Schema(description = "资产照片")
    private String assetPhoto;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "更新人")
    private String updatedBy;
}
