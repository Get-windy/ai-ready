package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城注册用户
 */
@Data
@TableName("shop_user")
public class ShopUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 登录名 */
    private String username;

    /** 加密密码 */
    private String password;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 公司名称（B2B） */
    private String companyName;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 注册来源 h5/wxapp */
    private String source;

    /** 审核状态 0=待审核 1=通过 2=驳回 */
    private Integer auditStatus;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核人 */
    private Long auditBy;

    /** 驳回原因 */
    private String rejectReason;

    /** 关联ERP客户ID */
    private Long erpCustomerId;

    /** 关联ERP往来单位ID */
    private Long erpPartnerId;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 1正常 0禁用 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
