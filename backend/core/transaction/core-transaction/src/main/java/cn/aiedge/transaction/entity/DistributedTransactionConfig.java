package cn.aiedge.transaction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 分布式事务配置实体
 * 存储分布式事务的相关配置信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("distributed_transaction_config")
public class DistributedTransactionConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 配置类型
     */
    private String configType;

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
     * 是否启用
     */
    private Boolean enabled;

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