package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * 批次查询DTO
 * 用于接收复杂查询条件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchQueryDTO {

    /**
     * 批次号（模糊查询）
     */
    private String batchNo;

    /**
     * 产品编码（精确查询）
     */
    private String productCode;

    /**
     * 产品名称（模糊查询）
     */
    private String productName;

    /**
     * 批次状态
     */
    private String batchStatus;

    /**
     * 质检状态
     */
    private String qualityStatus;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 生产订单号
     */
    private String productionOrderNo;

    /**
     * 采购订单号
     */
    private String purchaseOrderNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 生产日期开始
     */
    private LocalDate productionDateStart;

    /**
     * 生产日期结束
     */
    private LocalDate productionDateEnd;

    /**
     * 过期日期开始
     */
    private LocalDate expirationDateStart;

    /**
     * 过期日期结束
     */
    private LocalDate expirationDateEnd;

    /**
     * 是否只查询临期批次（过期日期在指定天数内）
     */
    private Boolean expiringOnly;

    /**
     * 临期预警天数（默认7天）
     */
    private Integer warningDays = 7;

    /**
     * 是否只查询过期批次
     */
    private Boolean expiredOnly;

    /**
     * 是否只查询可用批次（可用数量>0）
     */
    private Boolean availableOnly;

    /**
     * 排序字段（默认按创建时间倒序）
     */
    private String sortField = "createdAt";

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection = "DESC";

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 20;

    /**
     * 是否包含已删除数据
     */
    private Boolean includeDeleted = false;
}