package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 追溯查询日志表
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Data
@TableName("traceability_log")
public class TraceabilityLog {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 追溯类型：BATCH-批次, SERIAL-序列号, PRODUCT-产品
     */
    @TableField("trace_type")
    private String traceType;
    
    /**
     * 追溯对象ID
     */
    @TableField("trace_id")
    private Long traceId;
    
    /**
     * 追溯对象编码
     */
    @TableField("trace_code")
    private String traceCode;
    
    /**
     * 追溯对象名称
     */
    @TableField("trace_name")
    private String traceName;
    
    /**
     * 查询类型：SIMPLE-简单查询, DETAILED-详细查询, FULL_TRACE-全链路追溯
     */
    @TableField("query_type")
    private String queryType;
    
    /**
     * 查询时间范围起始
     */
    @TableField("query_time_range_start")
    private LocalDateTime queryTimeRangeStart;
    
    /**
     * 查询时间范围结束
     */
    @TableField("query_time_range_end")
    private LocalDateTime queryTimeRangeEnd;
    
    /**
     * 结果数量
     */
    @TableField("result_count")
    private Integer resultCount;
    
    /**
     * 查询耗时（毫秒）
     */
    @TableField("query_duration_ms")
    private Integer queryDurationMs;
    
    /**
     * 查询用户ID
     */
    @TableField("query_user_id")
    private String queryUserId;
    
    /**
     * 查询用户姓名
     */
    @TableField("query_user_name")
    private String queryUserName;
    
    /**
     * 查询IP
     */
    @TableField("query_ip")
    private String queryIp;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 业务方法：获取追溯类型枚举
     */
    public TraceType getTraceTypeEnum() {
        return TraceType.fromCode(traceType);
    }
    
    /**
     * 业务方法：获取查询类型枚举
     */
    public QueryType getQueryTypeEnum() {
        return QueryType.fromCode(queryType);
    }
    
    /**
     * 业务方法：记录查询耗时
     */
    public void recordQueryDuration(long startTime) {
        this.queryDurationMs = (int) (System.currentTimeMillis() - startTime);
    }
    
    /**
     * 追溯类型枚举
     */
    public enum TraceType {
        BATCH("BATCH", "批次"),
        SERIAL("SERIAL", "序列号"),
        PRODUCT("PRODUCT", "产品");
        
        private final String code;
        private final String description;
        
        TraceType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static TraceType fromCode(String code) {
            for (TraceType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
    
    /**
     * 查询类型枚举
     */
    public enum QueryType {
        SIMPLE("SIMPLE", "简单查询"),
        DETAILED("DETAILED", "详细查询"),
        FULL_TRACE("FULL_TRACE", "全链路追溯");
        
        private final String code;
        private final String description;
        
        QueryType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static QueryType fromCode(String code) {
            for (QueryType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
}