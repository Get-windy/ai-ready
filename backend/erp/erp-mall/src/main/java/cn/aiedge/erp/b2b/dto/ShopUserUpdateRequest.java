package cn.aiedge.erp.b2b.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 买家账号编辑请求（{@code PUT /api/erp/mall/admin/user/{id}}）。
 *
 * <p><b>为什么用专门 DTO 而不是直接用 {@code ShopUser} 接参：</b>实体接参会让前端可传
 * {@code id / tenantId / deleted / password / auditStatus / partyId} 等字段，造成越权改写
 * （改租户、改审核状态、改关联往来单位、直接改密码哈希）。本 DTO 只暴露「买家账号」页
 * 允许编辑的展示/归属字段。</p>
 *
 * <p><b>部分更新语义：</b>字段为 {@code null} 表示「不修改」；String 字段传空串 {@code ""}
 * 表示「清空」。唯一例外是成对提交的归属字段
 * （{@code defaultHandlerId/Name}、{@code warehouseId/Name}、{@code deptId/Name}）：
 * 只要 id 或 name 任一非 null 即视为「提交了该字段」，name 传空串同时清空 id（支持取消归属）。
 * 数值型 {@code categoryId} 为 null 表示不修改（仅支持改选，不支持清空）。</p>
 */
@Data
@Schema(description = "买家账号编辑请求（部分更新，null=不修改）")
public class ShopUserUpdateRequest {

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickname;

    /** 真实姓名 / 联系人姓名（shop_user.contact_name） */
    @Schema(description = "真实姓名（联系人姓名）")
    private String contactName;

    /** 手机号（登录名口径，买家登录名即手机号） */
    @Schema(description = "手机号")
    private String phone;

    /** 公司名称（企业客户） */
    @Schema(description = "公司名称")
    private String companyName;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** QQ */
    @Schema(description = "QQ")
    private String qq;

    /** 微信 */
    @Schema(description = "微信")
    private String wechat;

    /** 地址 */
    @Schema(description = "地址")
    private String address;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /**
     * 客户级别（shop_user.customer_level，存级别名称）。
     * 前端「客户级别」下拉取自 {@code GET /erp/partner/grades?gradeType=CUSTOMER}，
     * 提交时写级别名称，与 {@code /user/page?gradeId=} 过滤口径一致（按名称匹配）。
     */
    @Schema(description = "客户级别（级别名称，如「A餐饮客户」）")
    private String customerLevel;

    /** 归属分类ID（biz_party_category.id，party_type=CUSTOMER） */
    @Schema(description = "归属分类ID")
    private Long categoryId;

    /** 默认经手人ID（sys_user.id） */
    @Schema(description = "默认经手人ID")
    private Long defaultHandlerId;

    /** 默认经手人姓名 */
    @Schema(description = "默认经手人姓名")
    private String defaultHandlerName;

    /** 所属仓库ID（erp_warehouse.id） */
    @Schema(description = "所属仓库ID")
    private Long warehouseId;

    /** 所属仓库名称 */
    @Schema(description = "所属仓库名称")
    private String warehouseName;

    /** 所属部门ID（sys_dept.id） */
    @Schema(description = "所属部门ID")
    private Long deptId;

    /** 所属部门名称 */
    @Schema(description = "所属部门名称")
    private String deptName;
}
