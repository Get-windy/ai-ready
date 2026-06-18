package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 序列号状态快照缓存表
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Data
@TableName("serial_status_cache")
public class SerialStatusCache {
    
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
     * 快照日期
     */
    @TableField("snapshot_date")
    private LocalDate snapshotDate;
    
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
     * 状态：AVAILABLE/IN_USE/INSERVICE/MAINTAINED/SCRAP
     */
    @TableField("sn_status")
    private String snStatus;
    
    /**
     * 阶段：WAREHOUSE/IN_TRANSIT/EOF_CUSTOMER/IN_SERVICE/SCRAPPED
     */
    @TableField("sn_stage")
    private String snStage;
    
    /**
     * 当前位置
     */
    @TableField("current_location")
    private String currentLocation;
    
    /**
     * 质保结束日期
     */
    @TableField("warranty_end_date")
    private LocalDate warrantyEndDate;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 业务方法：判断是否为当日快照
     */
    public boolean isTodaySnapshot() {
        return snapshotDate != null && 
               snapshotDate.equals(LocalDate.now());
    }
    
    /**
     * 业务方法：判断是否在质保期内
     */
    public boolean isUnderWarranty() {
        if (warrantyEndDate == null) {
            return false;
        }
        return !LocalDate.now().isAfter(warrantyEndDate);
    }
    
    /**
     * 业务方法：获取剩余质保天数
     */
    public long getRemainingWarrantyDays() {
        if (warrantyEndDate == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(warrantyEndDate)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, warrantyEndDate);
    }
    
    /**
     * 业务方法：判断序列号是否可用
     */
    public boolean isAvailable() {
        return "AVAILABLE".equals(snStatus);
    }
    
    /**
     * 业务方法：判断序列号是否已报废
     */
    public boolean isScrapped() {
        return "SCRAP".equals(snStatus) || "SCRAPPED".equals(snStage);
    }
    
    /**
     * 业务方法：获取状态枚举
     */
    public SnStatus getSnStatusEnum() {
        return SnStatus.fromCode(snStatus);
    }
    
    /**
     * 业务方法：获取阶段枚举
     */
    public SnStage getSnStageEnum() {
        return SnStage.fromCode(snStage);
    }
    
    /**
     * 业务方法：获取快照摘要
     */
    public String getSnapshotSummary() {
        return String.format("序列号%s状态快照: 状态=%s, 阶段=%s, 位置=%s", 
            serialNo, snStatus, snStage, currentLocation);
    }
    
    /**
     * 序列号状态枚举
     */
    public enum SnStatus {
        AVAILABLE("AVAILABLE", "可用"),
        IN_USE("IN_USE", "使用中"),
        INSERVICE("INSERVICE", "在服务中"),
        MAINTAINED("MAINTAINED", "维护中"),
        SCRAP("SCRAP", "报废");
        
        private final String code;
        private final String description;
        
        SnStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static SnStatus fromCode(String code) {
            for (SnStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return null;
        }
    }
    
    /**
     * 序列号阶段枚举
     */
    public enum SnStage {
        WAREHOUSE("WAREHOUSE", "仓库中"),
        IN_TRANSIT("IN_TRANSIT", "运输中"),
        EOF_CUSTOMER("EOF_CUSTOMER", "终端客户"),
        IN_SERVICE("IN_SERVICE", "服务中"),
        SCRAPPED("SCRAPPED", "已报废");
        
        private final String code;
        private final String description;
        
        SnStage(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static SnStage fromCode(String code) {
            for (SnStage stage : values()) {
                if (stage.code.equals(code)) {
                    return stage;
                }
            }
            return null;
        }
    }
}