package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商城「系统顾客 × 租户」关联（V11.484.0 建表）。
 *
 * <p><b>它解决什么</b>：2026-09-22 用户裁定 —— 商城顾客是**系统级身份**
 * （{@link ShopUser}，相当于商城侧的 `sys_user`），而「某个租户的顾客」是**关联关系**
 * （本类，相当于 `sys_user_tenant`）。同一个系统顾客可被多个租户分别添加，
 * 前提是**该租户添加了他**。</p>
 *
 * <p><b>唯一事实来源</b>：判断"这个顾客属不属于本租户 / 能不能登录本店"**只能查本表**；
 * 不要用 {@code ShopUser#tenantId}（那是系统级归属位，恒 0）。</p>
 *
 * <p><b>审核是逐租户的</b>：同一个顾客在 A 店被批准、在 B 店被拒绝是合法状态，
 * 故审核字段在本表而不是 {@code shop_user.audit_status}（后者已降级为历史列）。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Data
@Accessors(chain = true)
@TableName("shop_user_tenant")
public class ShopUserTenant {

    /** 关联状态：待审核 */
    public static final int STATUS_PENDING = 0;
    /** 关联状态：正常（可登录本店） */
    public static final int STATUS_ACTIVE = 1;
    /** 关联状态：已拒绝 */
    public static final int STATUS_REJECTED = 2;
    /** 关联状态：已解除（原正常关系被撤销） */
    public static final int STATUS_REVOKED = 3;

    /** 来源：本店自助注册 */
    public static final String SOURCE_SELF_REGISTER = "self_register";
    /** 来源：复用已有系统顾客身份绑定到本店 */
    public static final String SOURCE_SYSTEM_REUSE = "system_reuse";
    /** 来源：租户后台添加 */
    public static final String SOURCE_TENANT_ADD = "tenant_add";

    /** 本店启用 */
    public static final int ENABLED_YES = 1;
    /** 本店停用（本店管理员操作，**只影响本店**） */
    public static final int ENABLED_NO = 0;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统顾客 id（{@code shop_user.id}） */
    private Long shopUserId;

    /** 租户 id —— "这个顾客属于哪家店"就是本列 */
    private Long tenantId;

    /** **准入审核**：0=待审核 1=正常 2=已拒绝 3=已解除 */
    private Integer status;

    /**
     * **本店启用状态**：1=启用 0=停用（本店管理员操作，**只影响本店**）。
     *
     * <p>与 {@link #status} 是**两个正交维度**：停用一个已通过的顾客，
     * 他的审核记录仍然是"已通过"。</p>
     *
     * <p>⚠️ 别和 {@code shop_user.status} 混：那是**平台级**账号开关（封号），
     * 影响该顾客在**所有店**；本列只影响本店。</p>
     */
    private Integer enabled;

    /** 来源：self_register / system_reuse / tenant_add */
    private String source;

    /** 该顾客在本店的默认身份标记 */
    private Integer isDefault;

    /** 申请时留的手机号（审核对照用） */
    private String appliedPhone;

    private Long auditBy;

    private LocalDateTime auditTime;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
