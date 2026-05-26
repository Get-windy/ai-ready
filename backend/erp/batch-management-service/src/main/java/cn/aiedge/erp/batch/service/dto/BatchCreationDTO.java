package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批次创建DTO
 * 用于接收批次创建请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreationDTO {

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /**
     * 产品编码
     */
    @NotBlank(message = "产品编码不能为空")
    private String productCode;

    /**
     * 产品名称
     */
    @NotBlank(message = "产品名称不能为空")
    private String productName;

    /**
     * 批次号（可选，为空时自动生成）
     */
    private String batchNo;

    /**
     * 生产日期
     */
    @NotNull(message = "生产日期不能为空")
    private LocalDate productionDate;

    /**
     * 过期日期
     */
    private LocalDate expirationDate;

    /**
     * 总数量
     */
    @NotNull(message = "总数量不能为空")
    @Min(value = 0, message = "总数量不能小于0")
    private BigDecimal totalQuantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 生产订单号
     */
    private String productionOrderNo;

    /**
     * 采购订单号
     */
    private String purchaseOrderNo;

    /**
     * 来源类型（生产、采购、退货等）
     */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 货位ID
     */
    private Long locationId;

    /**
     * 质检状态（默认待检）
     */
    private String qualityStatus = "PENDING";

    /**
     * 批次状态（默认ACTIVE）
     */
    private String batchStatus = "ACTIVE";
}