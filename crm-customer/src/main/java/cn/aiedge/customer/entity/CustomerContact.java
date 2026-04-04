package cn.aiedge.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 客户联系人实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("crm_customer_contact")
public class CustomerContact {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 联系人姓名 */
    private String contactName;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 职位 */
    private String position;

    /** 部门 */
    private String department;

    /** 是否关键决策人 */
    private Boolean isKeyDecisionMaker;

    /** 联系电话 */
    private String phone;

    /** 手机号码 */
    private String mobile;

    /** 电子邮箱 */
    private String email;

    /** 微信号 */
    private String wechat;

    /** QQ号 */
    private String qq;

    /** 生日 */
    private LocalDateTime birthday;

    /** 备注 */
    private String remark;

    /** 状态：0-启用 1-禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}