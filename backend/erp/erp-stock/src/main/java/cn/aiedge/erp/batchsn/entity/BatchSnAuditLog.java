package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 批次/序列号操作审计日志
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Data
@TableName("batchsn_audit_log")
public class BatchSnAuditLog {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 表名
     */
    @TableField("table_name")
    private String tableName;
    
    /**
     * 记录ID
     */
    @TableField("record_id")
    private Long recordId;
    
    /**
     * 操作类型：CREATE-创建, UPDATE-更新, DELETE-删除
     */
    @TableField("operation_type")
    private String operationType;
    
    /**
     * 变更前数据（JSON）
     */
    @TableField("old_data")
    private String oldData;
    
    /**
     * 变更后数据（JSON）
     */
    @TableField("new_data")
    private String newData;
    
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
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;
    
    /**
     * 业务方法：获取操作类型枚举
     */
    public OperationType getOperationTypeEnum() {
        return OperationType.fromCode(operationType);
    }
    
    /**
     * 业务方法：记录变更数据
     */
    public void recordChange(String oldDataJson, String newDataJson) {
        this.oldData = oldDataJson;
        this.newData = newDataJson;
        this.operationTime = LocalDateTime.now();
    }
    
    /**
     * 业务方法：获取审计摘要
     */
    public String getAuditSummary() {
        return String.format("%s表记录%d执行%s操作", 
            tableName, recordId, operationType);
    }
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CREATE("CREATE", "创建"),
        UPDATE("UPDATE", "更新"),
        DELETE("DELETE", "删除");
        
        private final String code;
        private final String description;
        
        OperationType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static OperationType fromCode(String code) {
            for (OperationType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
}