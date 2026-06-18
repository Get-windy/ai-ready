package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 序列号流转记录表
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Data
@TableName("serial_flow_record")
public class SerialFlowRecord {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 序列号ID
     */
    @TableField("serial_id")
    private Long serialId;
    
    /**
     * 序列号
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
     * 流转类型: INBOUND-入库, OUTBOUND-出库, TRANSFER-调拨, MAINTAIN-维修, SCRAP-报废
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
     * 变更前状态
     */
    @TableField("from_status")
    private String fromStatus;
    
    /**
     * 变更后状态
     */
    @TableField("to_status")
    private String toStatus;
    
    /**
     * 变更前阶段
     */
    @TableField("from_stage")
    private String fromStage;
    
    /**
     * 变更后阶段
     */
    @TableField("to_stage")
    private String toStage;
    
    /**
     * 变更前位置
     */
    @TableField("from_location")
    private String fromLocation;
    
    /**
     * 变更后位置
     */
    @TableField("to_location")
    private String toLocation;
    
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
