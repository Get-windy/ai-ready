package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次详情DTO
 * 用于返回批次详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDetailDTO {
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
    private BigDecimal totalQuantity;
    
    /**
     * 可用数量
     */
    private BigDecimal availableQuantity;
    
    /**
     * 预留数量
     */
    private BigDecimal reservedQuantity;
    
    /**
     * 锁定数量
     */
    private BigDecimal lockedQuantity;
    
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
     * 质检结果
     */
    private String qualityResult;
    
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
     * 货位编码
     */
    private String locationCode;
    
    /**
     * 货位名称
     */
    private String locationName;
    
    /**
     * 创建人ID
     */
    private String createdBy;
    
    /**
     * 创建人姓名
     */
    private String createdByName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新人ID
     */
    private String updatedBy;
    
    /**
     * 更新人姓名
     */
    private String updatedByName;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 扩展字段（JSON格式）
     */
    private String extraInfo;
    
    /**
     * 批次关联的生产批次号（如果是子批次）
     */
    private String parentBatchNo;
    
    /**
     * 批次关联的追溯码
     */
    private String traceCode;
    
    /**
     * 批次关联的条形码
     */
    private String barCode;
    
    /**
     * 批次图片URL
     */
    private String imageUrl;
    
    /**
     * 批次文档URL
     */
    private String documentUrl;
    
    /**
     * 批次成本单价
     */
    private BigDecimal unitCost;
    
    /**
     * 批次总成本
     */
    private BigDecimal totalCost;
    
    /**
     * 批次销售单价
     */
    private BigDecimal unitPrice;
    
    /**
     * 批次标签
     */
    private String tags;
    
    /**
     * 批次优先级（1-10）
     */
    private Integer priority;
    
    /**
     * 批次紧急程度
     */
    private String urgencyLevel;
    
    /**
     * 批次质量等级
     */
    private String qualityGrade;
    
    /**
     * 批次有效期（天）
     */
    private Integer shelfLifeDays;
    
    /**
     * 批次存储条件
     */
    private String storageCondition;
    
    /**
     * 批次处理状态
     */
    private String processingStatus;
    
    /**
     * 批次处理进度（0-100）
     */
    private Integer processingProgress;
    
    /**
     * 批次预警状态
     */
    private String warningStatus;
    
    /**
     * 批次预警信息
     */
    private String warningMessage;
}