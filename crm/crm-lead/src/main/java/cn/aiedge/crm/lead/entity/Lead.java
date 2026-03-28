package cn.aiedge.crm.lead.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 线索实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("crm_lead")
public class Lead {

    /**
     * 线索ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 线索名称
     */
    private String name;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 来源（1-网站 2-电话 3-展会 4-推荐 5-其他）
     */
    private Integer source;

    /**
     * 行业
     */
    private String industry;

    /**
     * 地区
     */
    private String region;

    /**
     * 地址
     */
    private String address;

    /**
     * 线索状态（0-新线索 1-跟进中 2-已转化 3-已关闭）
     */
    private Integer status;

    /**
     * 线索评分（0-100）
     */
    private Integer score;

    /**
     * 负责人ID
     */
    private Long ownerId;

    /**
     * 负责人名称
     */
    private String ownerName;

    /**
     * 下次跟进时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 最后跟进时间
     */
    private LocalDateTime lastFollowTime;

    /**
     * 跟进次数
     */
    private Integer followCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

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
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 转化的客户ID
     */
    private Long convertedCustomerId;

    /**
     * 转化时间
     */
    private LocalDateTime convertedTime;
}