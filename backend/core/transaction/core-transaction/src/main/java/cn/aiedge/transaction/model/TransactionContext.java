package cn.aiedge.transaction.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事务上下文模型
 * 包含事务执行所需的所有上下文信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class TransactionContext implements Serializable {

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
     * 事务模式
     */
    private String transactionMode; // TCC, SAGA, BEST_EFFORT

    /**
     * 事务类型
     */
    private String transactionType; // GLOBAL, BRANCH

    /**
     * 参与者服务信息
     */
    private ParticipantInfo participantInfo;

    /**
     * 业务数据
     */
    private Object businessData;

    /**
     * 补偿数据
     */
    private Object compensationData;

    /**
     * 超时时间
     */
    private LocalDateTime timeoutTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 事务属性
     */
    private Map<String, Object> attributes;

    /**
     * 参与者信息内部类
     */
    @Data
    @Accessors(chain = true)
    public static class ParticipantInfo implements Serializable {
        /**
         * 服务名称
         */
        private String serviceName;

        /**
         * 服务地址
         */
        private String serviceUrl;

        /**
         * 参与者类型
         */
        private String participantType;

        /**
         * 执行顺序
         */
        private Integer executionOrder;

        /**
         * 回滚方法
         */
        private String rollbackMethod;

        /**
         * 重试配置
         */
        private RetryConfig retryConfig;
    }

    /**
     * 重试配置内部类
     */
    @Data
    @Accessors(chain = true)
    public static class RetryConfig implements Serializable {
        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;

        /**
         * 重试间隔(毫秒)
         */
        private Long retryInterval = 1000L;

        /**
         * 指数退避倍数
         */
        private Double backoffMultiplier = 2.0;
    }
}