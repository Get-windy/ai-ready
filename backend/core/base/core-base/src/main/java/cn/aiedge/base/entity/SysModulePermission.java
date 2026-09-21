package cn.aiedge.base.entity;

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
 * 「模块 → 权限码前缀」映射（V11.454.0 建表）。
 *
 * <p><b>它解决什么</b>：本仓授权是两层 —— 模块授权（平台方决定某租户有没有这个模块）
 * 与权限（租户内管理员决定某角色能不能做某件事）。两层要能对上，就得知道
 * 「关掉仓储模块」到底该关掉哪些权限码；本表就是那张对照表。</p>
 *
 * <p><b>归属判定口径</b>：<b>最长前缀优先，同长取 sort 小</b>。
 * 该口径由 {@code cn.aiedge.module.service.ModuleEntitlementService} 实现，
 * 并由 {@code tools/verify-module-mapping.cjs} 断言「无同长前缀歧义」。</p>
 *
 * <p><b>为什么 tenant_id 恒为 0</b>：这是**平台级参考数据**（跟 {@code sys_menu} /
 * {@code sys_permission} 同性质），不是某个租户的业务数据。也正因如此，
 * 本表已登记进 {@code MyBatisPlusConfig#IGNORE_TENANT_TABLES} ——
 * 否则租户会话读它会自动带上 {@code AND tenant_id = <会话租户>}，
 * 拿不到任何映射行，模块门就会**静默失效（fail-open）**。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Data
@Accessors(chain = true)
@TableName("sys_module_permission")
public class SysModulePermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属租户：恒为 0（平台级参考数据） */
    private Long tenantId;

    /** 模块编码，取值须存在于 {@code sys_module.module_code} */
    private String moduleCode;

    /** 权限码前缀，如 {@code wms:}、{@code erp:product:} */
    private String permissionPrefix;

    /** 同长前缀并列时的决胜值（越小越优先） */
    private Integer sort;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
