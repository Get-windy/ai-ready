package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批次流转记录表
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Data
@TableName("batch_flow_record")
public class BatchFlowRecord {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
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
     * 流转类型: INBOUND-入库, OUTBOUND-出库, TRANSFER-调拨, ADJUST-库存调整
     */
    @TableField("flow_type")
    private String flowType;
    
    /**
     * 流转单号
     */
    @TableField("flow_no")
    private String flowNo;
    
    /**
     * 流转单ID
     */
    @TableField("flow_id")
    private Long flowId;
    
    /**
     * 数量变化量（正数入库/减少预留，负数出库/增加预留）
     */
    @TableField("quantity_change")
    private BigDecimal quantityChange;
    
    /**
     * 变化前数量
     */
    @TableField("before_quantity")
    private BigDecimal beforeQuantity;
    
    /**
     * 变化后数量
     */
    @TableField("after_quantity")
    private BigDecimal afterQuantity;
    
    /**
     * 源仓库ID
     */
    @TableField("from_warehouse_id")
    private Long fromWarehouseId;
    
    /**
     * 源仓库名称
     */
    @TableField("from_warehouse_name")
    private String fromWarehouseName;
    
    /**
     * 目标仓库ID
     */
    @TableField("to_warehouse_id")
    private Long toWarehouseId;
    
    /**
     * 目标仓库名称
     */
    @TableField("to_warehouse_name")
    private String toWarehouseName;
    
    /**
     * 源库位ID
     */
    @TableField("from_location_id")
    private Long fromLocationId;
    
    /**
     * 目标库位ID
     */
    @TableField("to_location_id")
    private Long toLocationId;
    
    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private String operatorId;
    
    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;
    
    /**
     * 操作IP
     */
    @TableField("operator_ip")
    private String operatorIp;
    
    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 乐观锁版本
     */
    @TableField("version")
    @Version
    private Integer version;
}
