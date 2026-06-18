package cn.aiedge.erp.batchsn.controller.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Date;

/**
 * 批次高级搜索请求
 * 
 * @author team-member
 * @date 2026-05-05
 */
@Data
public class BatchAdvancedSearchRequest {
    
    /**
     * 批次号（模糊匹配）
     */
    private String batchNo;
    
    /**
     * 产品编码
     */
    private String productCode;
    
    /**
     * 产品名称（模糊匹配）
     */
    private String productName;
    
    /**
     * 批次状态
     */
    private String status;
    
    /**
     * 质量状态
     */
    private String qualityStatus;
    
    /**
     * 来源类型
     */
    private String sourceType;
    
    /**
     * 生产日期起始
     */
    private Date productionDateStart;
    
    /**
     * 生产日期结束
     */
    private Date productionDateEnd;
    
    /**
     * 过期日期起始
     */
    private Date expirationDateStart;
    
    /**
     * 过期日期结束
     */
    private Date expirationDateEnd;
    
    /**
     * 仓库ID
     */
    private Long warehouseId;
    
    /**
     * 仓库名称（模糊匹配）
     */
    private String warehouseName;
    
    /**
     * 排序字段（默认：createdAt）
     * 支持字段：createdAt, updatedAt, batchNo, productCode, productionDate, expirationDate
     */
    private String sortField = "createdAt";
    
    /**
     * 排序方向：asc, desc（默认：desc）
     */
    private String sortDirection = "desc";
    
    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    private int page = 1;
    
    /**
     * 每页数量（默认20，最大100）
     */
    @Min(value = 1, message = "每页数量必须大于0")
    private int size = 20;
    
    /**
     * 是否导出数据
     */
    private boolean export = false;
    
    /**
     * 验证并规范化请求参数
     */
    public void validate() {
        // 页码和页大小验证
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100; // 限制最大页大小
        }
        
        // 排序方向验证
        if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            sortDirection = "desc";
        }
        
        // 排序字段白名单验证
        String[] allowedSortFields = {"createdAt", "updatedAt", "batchNo", "productCode", "productionDate", "expirationDate"};
        boolean isValidSortField = false;
        for (String field : allowedSortFields) {
            if (field.equals(sortField)) {
                isValidSortField = true;
                break;
            }
        }
        if (!isValidSortField) {
            sortField = "createdAt";
        }
        
        // 日期范围验证
        if (productionDateStart != null && productionDateEnd != null) {
            if (productionDateStart.after(productionDateEnd)) {
                throw new IllegalArgumentException("生产日期起始时间不能晚于结束时间");
            }
        }
        if (expirationDateStart != null && expirationDateEnd != null) {
            if (expirationDateStart.after(expirationDateEnd)) {
                throw new IllegalArgumentException("过期日期起始时间不能晚于结束时间");
            }
        }
    }
}