package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 序列号主表
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Data
@TableName("serial_number")
public class SerialNumber {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 序列号（全球唯一）
     */
    @TableField("serial_no")
    private String serialNo;
    
    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;
    
    /**
     * 产品编码
     */
    @TableField("product_code")
    private String productCode;
    
    /**
     * 产品名称
     */
    @TableField("product_name")
    private String productName;
    
    /**
     * 产品规格
     */
    @TableField("specification")
    private String specification;
    
    /**
     * 批次ID
     */
    @TableField("batch_id")
    private Long batchId;
    
    /**
     * 批次号
     */
    @TableField("batch_no")
    private String batchNo;
    
    /**
     * 序列号状态: AVAILABLE-可用, IN_USE-使用中, INSERVICE-服务中, MAINTAINED-维修中, SCRAP-报废
     */
    @TableField("sn_status")
    private String snStatus;
    
    /**
     * 所处阶段: WAREHOUSE-仓库, IN_TRANSIT-在途, EOF_CUSTOMER-客户, IN_SERVICE-服务中, SCRAPPED-报废
     */
    @TableField("sn_stage")
    private String snStage;
    
    /**
     * 制造商
     */
    @TableField("manufacturer")
    private String manufacturer;
    
    /**
     * 生产日期
     */
    @TableField("manufacturing_date")
    private LocalDate manufacturingDate;
    
    /**
     * 质保期(月)
     */
    @TableField("warranty_period")
    private Integer warrantyPeriod;
    
    /**
     * 质保开始日期
     */
    @TableField("warranty_start_date")
    private LocalDate warrantyStartDate;
    
    /**
     * 质保结束日期
     */
    @TableField("warranty_end_date")
    private LocalDate warrantyEndDate;
    
    /**
     * 当前位置
     */
    @TableField("current_location")
    private String currentLocation;
    
    /**
     * 库位ID
     */
    @TableField("location_id")
    private Long locationId;
    
    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;
    
    /**
     * 采购订单ID
     */
    @TableField("purchase_order_id")
    private Long purchaseOrderId;
    
    /**
     * 采购订单号
     */
    @TableField("purchase_order_no")
    private String purchaseOrderNo;
    
    /**
     * 销售订单ID
     */
    @TableField("sale_order_id")
    private Long saleOrderId;
    
    /**
     * 销售订单号
     */
    @TableField("sale_order_no")
    private String saleOrderNo;
    
    /**
     * 质量状态: NORMAL-正常, DEFECTIVE-不合格, UNDER_REPAIR-维修中
     */
    @TableField("quality_status")
    private String qualityStatus;
    
    /**
     * 最后质检日期
     */
    @TableField("last_inspection_date")
    private LocalDateTime lastInspectionDate;
    
    /**
     * 下次质检日期
     */
    @TableField("next_inspection_date")
    private LocalDateTime nextInspectionDate;
    
    /**
     * 维修次数
     */
    @TableField("maintenance_count")
    private Integer maintenanceCount;
    
    /**
     * 最后维修日期
     */
    @TableField("last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;
    
    /**
     * 创建人ID
     */
    @TableField("created_by")
    private String createdBy;
    
    /**
     * 创建人姓名
     */
    @TableField("created_by_name")
    private String createdByName;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private String updatedBy;
    
    /**
     * 更新人姓名
     */
    @TableField("updated_by_name")
    private String updatedByName;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 删除时间（软删除）
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 乐观锁版本
     */
    @TableField("version")
    @Version
    private Integer version;
    
    /**
     * 逻辑删除标记
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
