package cn.aiedge.transaction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 分布式事务参与者实体
 * 记录参与分布式事务的服务节点信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("distributed_transaction_participant")
public class DistributedTransactionParticipant {

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
     * 参与者服务名称
     */
    private String serviceName;

    /**
     * 参与者服务地址
     */
    private String serviceUrl;

    /**
     * 参与者状态
     * 0-未执行, 1-执行中, 2-执行成功, 3-执行失败, 4-已回滚
     */
    private Integer status;

    /**
     * 参与者类型
     * TCC_TRY, TCC_CONFIRM, TCC_CANCEL, SAGA_FORWARD, SAGA_COMPENSATE
     */
    private String participantType;

    /**
     * 执行顺序
     */
    private Integer executionOrder;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 超时时间
     */
    private LocalDateTime timeoutTime;

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
     * 执行结果
     */
    private String executionResult;

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