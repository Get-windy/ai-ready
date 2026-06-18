package cn.aiedge.dms.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配送渠道配置
 */
@Data
@TableName("dms_channel")
public class DmsChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 渠道编码：own/staff/meituan/dada/shunfeng/taxi */
    private String channelCode;

    /** 渠道名称 */
    private String channelName;

    /** 类型：1-自有员工 2-众包兼职 3-外部平台 4-社会车辆 */
    private Integer channelType;

    /** 适配器Spring Bean名称 */
    private String adapterBean;

    /** 渠道配置JSON（API密钥、回调URL等） */
    private String configJson;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 调度优先级（数字越小优先级越高） */
    private Integer priority;

    private Integer sortOrder;
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
