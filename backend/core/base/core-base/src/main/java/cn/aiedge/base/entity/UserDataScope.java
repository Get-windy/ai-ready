package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作员数据权限（对象清单授权）
 * <p>
 * 对标 ql361「资料 → 职员权限 → 全部操作员」页的 7 类数据权限：
 * 仓库 / 调拨 / 部门 / 往来单位 / 商品 / 现金银行 / 客户级别。
 * 每行表示「某操作员 × 某维度 → 一份授权对象 id 清单」。
 * </p>
 * <p>
 * 与 {@link SysDataScope}（角色 + 规则类型 + 部门ID，行级过滤规则）语义不同：
 * 本表是**对象清单授权**，两张旧表保持原样不复用。
 * </p>
 * <p>
 * 公共字段（id/tenant_id/create_time/update_time/create_by/update_by/deleted/version）
 * 全部来自 {@link BaseEntity}，此处不再重复声明。
 * </p>
 * <p>
 * 多租户：本表有 tenant_id 且**不在** MyBatisPlusConfig.IGNORE_TENANT_TABLES 白名单内，
 * 租户条件由 MyBatis-Plus 租户拦截器自动注入，业务代码不要手写 tenant_id 条件。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_data_scope")
public class UserDataScope extends BaseEntity {

    /** 操作员ID（sys_user.id） */
    private Long userId;

    /** 权限维度：warehouse/transfer/department/partner/product/fund/customer_level */
    private String scopeKey;

    /** 授权对象 id 列表（逗号分隔）；空/NULL 表示该维度未设置 */
    private String targetIds;
}
