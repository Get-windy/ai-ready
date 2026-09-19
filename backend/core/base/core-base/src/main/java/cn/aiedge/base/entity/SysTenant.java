package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户实体（身份 / 生命周期，**不含**企业档案字段）
 *
 * <p><b>字段边界（2026-09-18 拆表后，务必遵守）</b>：</p>
 * <ul>
 *   <li>本实体 = 平台侧「租户管理」与生命周期：tenantCode / adminUserId / level / expireTime /
 *       status / remark，以及租户名与**既有**的租户联系方式（contactPerson / contactPhone /
 *       contactEmail / address）。共 15 列，由平台侧与租户注册流程维护。</li>
 *   <li>租户侧「企业档案」（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）的
 *       15 个档案列 + LOGO **已迁到 1:1 子表** {@link SysTenantProfile}
 *       （`sys_tenant_profile`，迁移 V11.420.0）。</li>
 * </ul>
 *
 * <p><b>为什么拆</b>：上一轮（V11.396.0）为让企业信息页 15 个表单项有落点，直接给本表加了
 * 15 列 → 全表 30 列，违反《开发技术规范》「单表 ≤25 列 / 禁止上帝表」。
 * 拆表后本表回到 15 列；两张表通过 {@code sys_tenant_profile.tenant_id = sys_tenant.id} 1:1 关联。</p>
 *
 * <p>注意：`sys_tenant` **无 tenant_id 列**（已在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 中），
 * 故「企业信息」页的租户归属由会话（SecurityContext）显式确定，不依赖多租户插件注入；</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_tenant")
public class SysTenant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantName;

    private String tenantCode;

    /**
     * 联系人（**既有列**：租户注册 / 租户管理在用，非企业信息页新增）
     */
    private String contactPerson;

    /**
     * 联系电话（**既有列**）
     */
    private String contactPhone;

    /**
     * 企业邮箱（**既有列**）
     */
    private String contactEmail;

    /**
     * 企业地址（**既有列**）
     */
    private String address;

    /**
     * 租户管理员用户ID
     */
    private Long adminUserId;

    /**
     * 租户等级（basic-基础版 professional-专业版 enterprise-企业版）
     */
    private String level;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
