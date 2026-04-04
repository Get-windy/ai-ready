package cn.aiedge.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 客户跟进记录实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("crm_customer_follow")
public class CustomerFollow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户ID */
    private Long customerId;

    /** 跟进类型：1-电话 2-上门 3-微信 4-邮件 5-其他 */
    private Integer followType;

    /** 跟进内容 */
    private String content;

    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;

    /** 跟进结果：1-成功 2-失败 3-待定 */
    private Integer result;

    /** 跟进阶段变更 */
    private Integer stageFrom;

    private Integer stageTo;

    /** 跟进人ID */
    private Long followerId;

    /** 跟进人名称 */
    private String followerName;

    /** 附件（JSON） */
    private String attachments;

    /** 跟进时间 */
    private LocalDateTime followTime;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}