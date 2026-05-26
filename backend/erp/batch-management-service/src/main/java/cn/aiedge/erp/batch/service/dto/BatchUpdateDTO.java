package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批次更新DTO
 * 用于接收批次更新请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateDTO {

    /**
     * 批次ID
     */
    private Long id;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 过期日期
     */
    private LocalDate expirationDate;

    /**
     * 总数量
     */
    @Min(value = 0, message = "总数量不能小于0")
    private BigDecimal totalQuantity;

    /**
     * 可用数量
     */
    @Min(value = 0, message = "可用数量不能小于0")
    private BigDecimal availableQuantity;

    /**
     * 预留数量
     */
    @Min(value = 0, message = "预留数量不能小于0")
    private BigDecimal reservedQuantity;

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
     * 来源类型
     */
    private String sourceType;

    /**
     * 批次状态
     */
    private String batchStatus;

    /**
     * 质检状态
     */
    private String qualityStatus;

    /**
     * 质检员ID
     */
    private String qualityInspectorId;

    /**
     * 质检员名称
     */
    private String qualityInspectorName;

    /**
     * 质检日期
     */
    private LocalDate qualityInspectionDate;

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
     * 备注
     */
    private String remark;

    /**
     * 是否删除
     */
    private Integer isDeleted = 0;

    /**
     * 版本号（用于乐观锁）
     */
    private Integer version = 0;
}