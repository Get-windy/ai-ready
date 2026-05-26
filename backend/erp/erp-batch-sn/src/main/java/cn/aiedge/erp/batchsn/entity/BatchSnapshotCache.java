package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批次快照缓存表（定时刷新）
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Data
@TableName("batch_snapshot_cache")
public class BatchSnapshotCache {
    
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
     * 快照日期
     */
    @TableField("snapshot_date")
    private LocalDate snapshotDate;
    
    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;
    
    /**
     * 批次号
     */
    @TableField("batch_no")
    private String batchNo;
    
    /**
     * 总数量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;
    
    /**
     * 可用数量
     */
    @TableField("available_quantity")
    private BigDecimal availableQuantity;
    
    /**
     * 预留数量
     */
    @TableField("reserved_quantity")
    private BigDecimal reservedQuantity;
    
    /**
     * 质量状态
     */
    @TableField("quality_status")
    private String qualityStatus;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 业务方法：计算已使用数量
     */
    public BigDecimal getUsedQuantity() {
        if (totalQuantity == null || availableQuantity == null) {
            return BigDecimal.ZERO;
        }
        return totalQuantity.subtract(availableQuantity);
    }
    
    /**
     * 业务方法：计算可用率
     */
    public BigDecimal getAvailabilityRate() {
        if (totalQuantity == null || totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (availableQuantity == null) {
            return BigDecimal.ZERO;
        }
        return availableQuantity.divide(totalQuantity, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * 业务方法：判断是否为当日快照
     */
    public boolean isTodaySnapshot() {
        return snapshotDate != null && 
               snapshotDate.equals(LocalDate.now());
    }
    
    /**
     * 业务方法：获取快照摘要
     */
    public String getSnapshotSummary() {
        return String.format("批次%s(%s)快照: 总量=%s, 可用=%s", 
            batchNo, snapshotDate, totalQuantity, availableQuantity);
    }
}