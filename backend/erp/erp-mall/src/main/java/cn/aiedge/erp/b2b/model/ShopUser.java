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

    /** 关联ERP客户ID（已废弃，使用 partyId） */
    private Long erpCustomerId;

    /** 关联ERP往来单位ID（已废弃，使用 partyId） */
    private Long erpPartnerId;

    /**
     * 用户身份类型（统一登录核心字段）
     * ENTERPRISE = 企业客户 → 走B2B销售订单，合同价/等级价，账期结算
     * MEMBER     = 个人会员 → 走零售/商城订单，零售价/会员价，即时支付
     */
    private String userType;

    /** 统一身份标识 → biz_party.id */
    private Long partyId;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    // ═══ 买家申请管理 / 买家账号 页字段（views/mall/buyer-apply、views/mall/buyer-account）═══

    /** 联系人姓名 */
    @TableField("contact_name")
    private String contactName;

    /** 地址 */
    @TableField("address")
    private String address;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 营业执照图片URL */
    @TableField("business_license")
    private String businessLicense;

    /** QQ */
    @TableField("qq")
    private String qq;

    /** 微信 */
    @TableField("wechat")
    private String wechat;

    /** 归属分类ID（biz_party_category.id，party_type=CUSTOMER） */
    @TableField("category_id")
    private Long categoryId;

    /** 默认经手人ID（sys_user.id） */
    @TableField("default_handler_id")
    private Long defaultHandlerId;

    /** 默认经手人姓名 */
    @TableField("default_handler_name")
    private String defaultHandlerName;

    /** 客户级别 */
    @TableField("customer_level")
    private String customerLevel;

    /** 所属仓库ID（erp_warehouse.id） */
    @TableField("warehouse_id")
    private Long warehouseId;

    /** 所属仓库名称 */
    @TableField("warehouse_name")
    private String warehouseName;

    /** 所属部门ID（sys_dept.id） */
    @TableField("dept_id")
    private Long deptId;

    /** 所属部门名称 */
    @TableField("dept_name")
    private String deptName;

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
