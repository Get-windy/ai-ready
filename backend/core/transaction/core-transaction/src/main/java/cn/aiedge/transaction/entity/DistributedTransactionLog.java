package cn.aiedge.transaction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 分布式事务日志实体
 * 记录分布式事务的执行状态和相关信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("distributed_transaction_log")
public class DistributedTransactionLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 全局事务ID
     */
    private String globalTxId;

    /**
     * 分支事务ID
     */
    private String branchTxId;

    /**
     * 事务名称
     */
    private String transactionName;

    /**
     * 事务状态
     * 0-初始状态, 1-尝试中, 2-确认, 3-取消, 4-失败, 5-超时
     */
    private Integer status;

    /**
     * 事务模式
     * TCC, SAGA, BEST_EFFORT
     */
    private String transactionMode;

    /**
     * 事务类型
     * ROOT, BRANCH
     */
    private String transactionType;

    /**
     * 参与者服务列表
     */
    private String participantServices;

    /**
     * 事务上下文数据(JSON格式)
     */
    private String transactionContext;

    /**
     * 业务数据(JSON格式)
     */
    private String businessData;

    /**
     * 补偿数据(JSON格式)
     */
    private String compensationData;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 超时时间
     */
    private LocalDateTime timeoutTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 版本号(乐观锁)
     */
    @Version
    private Integer version;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}