package cn.aiedge.erp.batchsn.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 序列号查询请求 DTO
 * 
 * @author team-member
 * @date 2026-05-05
 */
@Data
@Schema(description = "序列号查询请求参数")
public class SerialQueryRequest {
    
    @Schema(description = "序列号编号（支持模糊查询）", example = "SN20260505001")
    @Size(max = 100, message = "序列号编号长度不能超过100字符")
    private String serialNo;
    
    @Schema(description = "产品编码", example = "P-001")
    @Size(max = 50, message = "产品编码长度不能超过50字符")
    private String productCode;
    
    @Schema(description = "序列号状态", example = "AVAILABLE", allowableValues = {"AVAILABLE", "IN_USE", "MAINTAINED", "SCRAP"})
    private String status;
    
    @Schema(description = "批次号", example = "BATCH-20260505-001")
    @Size(max = 50, message = "批次号长度不能超过50字符")
    private String batchNo;
    
    @Schema(description = "仓库ID", example = "1")
    private Long warehouseId;
    
    @Schema(description = "库位ID", example = "101")
    private Long locationId;
    
    @Schema(description = "质保开始日期-起始", example = "2026-01-01")
    private LocalDate warrantyStartFrom;
    
    @Schema(description = "质保开始日期-结束", example = "2026-12-31")
    private LocalDate warrantyStartTo;
    
    @Schema(description = "质保到期日期-起始", example = "2026-06-01")
    private LocalDate warrantyExpireFrom;
    
    @Schema(description = "质保到期日期-结束", example = "2026-12-31")
    private LocalDate warrantyExpireTo;
    
    @Schema(description = "创建时间-起始", example = "2026-01-01")
    private LocalDate createdFrom;
    
    @Schema(description = "创建时间-结束", example = "2026-12-31")
    private LocalDate createdTo;
    
    @Schema(description = "最后更新时间-起始", example = "2026-01-01")
    private LocalDate updatedFrom;
    
    @Schema(description = "最后更新时间-结束", example = "2026-12-31")
    private LocalDate updatedTo;
    
    @Schema(description = "排序字段", example = "serialNo", allowableValues = {"serialNo", "productCode", "createdTime", "updatedTime", "warrantyExpireDate"})
    private String sortBy = "serialNo";
    
    @Schema(description = "排序方向", example = "ASC", allowableValues = {"ASC", "DESC"})
    private String sortDirection = "ASC";
    
    @Schema(description = "页码（从1开始）", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;
    
    @Schema(description = "每页大小", example = "20", defaultValue = "20")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 200, message = "每页大小最大为200")
    private int size = 20;
    
    /**
     * 验证并规范化请求参数
     */
    public void validate() {
        // 确保页码和大小在合理范围内
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 200) size = 200;
        
        // 确保排序字段有效
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "serialNo";
        }
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "ASC";
        }
        sortDirection = sortDirection.toUpperCase();
        if (!sortDirection.equals("ASC") && !sortDirection.equals("DESC")) {
            sortDirection = "ASC";
        }
        
        // 日期范围验证：确保起始日期不晚于结束日期
        if (warrantyStartFrom != null && warrantyStartTo != null && warrantyStartFrom.isAfter(warrantyStartTo)) {
            LocalDate temp = warrantyStartFrom;
            warrantyStartFrom = warrantyStartTo;
            warrantyStartTo = temp;
        }
        
        if (warrantyExpireFrom != null && warrantyExpireTo != null && warrantyExpireFrom.isAfter(warrantyExpireTo)) {
            LocalDate temp = warrantyExpireFrom;
            warrantyExpireFrom = warrantyExpireTo;
            warrantyExpireTo = temp;
        }
        
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            LocalDate temp = createdFrom;
            createdFrom = createdTo;
            createdTo = temp;
        }
        
        if (updatedFrom != null && updatedTo != null && updatedFrom.isAfter(updatedTo)) {
            LocalDate temp = updatedFrom;
            updatedFrom = updatedTo;
            updatedTo = temp;
        }
    }
    
    /**
     * 计算分页偏移量
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}