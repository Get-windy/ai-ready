package cn.aiedge.erp.batchsn.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 批次查询请求DTO
 *
 * @author team-member
 * @date 2026-05-05
 */
@Data
public class BatchQueryRequest {

    /**
     * 批次编号
     */
    @Size(max = 50, message = "批次编号长度不能超过50个字符")
    private String batchNo;

    /**
     * 产品编码
     */
    @Size(max = 50, message = "产品编码长度不能超过50个字符")
    private String productCode;

    /**
     * 批次状态
     */
    @Size(max = 20, message = "批次状态长度不能超过20个字符")
    private String status;

    /**
     * 来源类型
     */
    @Size(max = 20, message = "来源类型长度不能超过20个字符")
    private String sourceType;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 生产日期开始
     */
    private String productionDateStart;

    /**
     * 生产日期结束
     */
    private String productionDateEnd;

    /**
     * 有效期开始
     */
    private String expirationDateStart;

    /**
     * 有效期结束
     */
    private String expirationDateEnd;

    /**
     * 质检状态
     */
    @Size(max = 20, message = "质检状态长度不能超过20个字符")
    private String qualityStatus;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /**
     * 每页记录数
     */
    @Min(value = 1, message = "每页记录数必须大于等于1")
    @Min(value = 100, message = "每页记录数不能超过100")
    private Integer size = 20;

    /**
     * 排序字段
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortDirection = "DESC";

    /**
     * 是否只查询可用批次
     */
    private Boolean availableOnly = false;

    /**
     * 是否只查询临期批次
     */
    private Boolean expiringOnly = false;

    /**
     * 临期预警天数
     */
    @Min(value = 1, message = "临期预警天数必须大于等于1")
    private Integer warningDays = 30;

    /**
     * 验证查询参数
     */
    public void validate() {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        if (warningDays == null || warningDays < 1) {
            warningDays = 30;
        }
        if (!"ASC".equalsIgnoreCase(sortDirection) && !"DESC".equalsIgnoreCase(sortDirection)) {
            sortDirection = "DESC";
        }
    }
}