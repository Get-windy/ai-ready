package cn.aiedge.erp.purchase.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 采购需求查询DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDemandQueryDTO {
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 需求编号
     */
    private String demandNo;
    
    /**
     * 需求标题
     */
    private String title;
    
    /**
     * 物料编码
     */
    private String materialCode;
    
    /**
     * 物料名称
     */
    private String materialName;
    
    /**
     * 需求部门ID
     */
    private Long departmentId;
    
    /**
     * 需求人ID
     */
    private Long demandUserId;
    
    /**
     * 需求状态
     */
    private Integer status;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 审批状态
     */
    private Integer approvalStatus;
    
    /**
     * 创建时间范围 - 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;
    
    /**
     * 创建时间范围 - 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;
    
    /**
     * 期望到货时间范围 - 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedDeliveryDateStart;
    
    /**
     * 期望到货时间范围 - 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedDeliveryDateEnd;
    
    /**
     * 是否包含已删除
     */
    private Boolean includeDeleted;
    
    /**
     * 是否紧急需求
     */
    private Boolean urgent;
    
    /**
     * 关键词搜索（模糊匹配标题、物料名称、需求编号）
     */
    private String keyword;
    
    /**
     * 排序字段（默认按创建时间倒序）
     */
    private String sortField = "create_time";
    
    /**
     * 排序方向（asc/desc）
     */
    private String sortDirection = "desc";
    
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 20;
    
    /**
     * 导出格式（csv, excel, pdf）
     */
    private String exportFormat;
}