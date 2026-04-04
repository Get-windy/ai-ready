package cn.aiedge.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 客户档案实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("crm_customer")
public class Customer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 客户简称 */
    private String shortName;

    /** 客户类型：1-企业 2-个人 */
    private Integer customerType;

    /** 客户来源：1-线上 2-线下 3-转介绍 4-其他 */
    private Integer source;

    /** 客户行业 */
    private String industry;

    /** 客户级别：1-普通 2-重要 3-VIP */
    private Integer level;

    /** 客户阶段：1-潜在 2-意向 3-成交 4-流失 */
    private Integer stage;

    /** 负责人ID */
    private Long ownerId;

    /** 负责人名称 */
    private String ownerName;

    /** 所属部门ID */
    private Long deptId;

    /** 联系电话 */
    private String phone;

    /** 手机号码 */
    private String mobile;

    /** 电子邮箱 */
    private String email;

    /** 传真 */
    private String fax;

    /** 公司网址 */
    private String website;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区县 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 邮编 */
    private String zipCode;

    /** 备注 */
    private String remark;

    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;

    /** 最后跟进时间 */
    private LocalDateTime lastFollowTime;

    /** 跟进次数 */
    private Integer followCount;

    /** 成交金额 */
    private java.math.BigDecimal dealAmount;

    /** 成交次数 */
    private Integer dealCount;

    /** 扩展信息 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /** 状态：0-启用 1-禁用 */
    private Integer status;

    /** 是否删除 */
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
}