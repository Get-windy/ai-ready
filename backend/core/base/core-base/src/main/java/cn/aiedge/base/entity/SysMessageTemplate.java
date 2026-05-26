package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_message_template")
public class SysMessageTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 消息类型 1-邮件 2-站内信 3-SMS 4-企业微信
     */
    private Integer msgType;

    /**
     * 模板标题
     */
    private String title;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 内容类型 text/html/markdown
     */
    private String contentType;

    /**
     * 使用场景说明
     */
    private String description;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
