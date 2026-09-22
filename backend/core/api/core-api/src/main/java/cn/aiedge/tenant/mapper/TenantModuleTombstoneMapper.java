package cn.aiedge.tenant.mapper;

import cn.aiedge.base.entity.SysTenantModule;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 租户模块开通记录的「含墓碑行」读写 Mapper —— 平台侧开通/停用专用（平台-MODULE-01 遗留①，2026-09-22）。
 *
 * <p><b>为什么不能直接用 {@code SysTenantModuleMapper}（BaseMapper）？</b>
 * {@code SysTenantModule.deleted} 带 {@code @TableLogic}，MyBatis-Plus 会给所有
 * 由它生成的 CRUD 方法自动追加 {@code AND deleted = 0}（UPDATE 也追加）。
 * 于是「重新给某租户开通一个此前停用过的模块」这个动作在 BaseMapper 视野里：
 * <ul>
 *   <li>{@code selectOne(...)} 查不到那行墓碑（deleted = 1） ⇒ 误判为「首次开通」；</li>
 *   <li>即便拿到了 id，{@code updateById(...)} 也只会更新 0 行（附带 where deleted = 0）；</li>
 *   <li>结果就是 INSERT 第二行 —— 而 {@code sys_tenant_module} 上
 *       <b>实测只有主键索引，没有 (tenant_id, module_code) 唯一索引</b>
 *       （2026-09-22 实测 {@code pg_indexes}：仅有 {@code sys_tenant_module_pkey (id)}），
 *       第二行会**静默落库、不报任何错**，比撞唯一键更难发现：</li>
 * </ul>
 * 而 {@code TenantModuleService.removeModule} 的软删是按 (tenant_id, module_code) 批量置 deleted = 1 的
 * ⇒ 反复「停用 → 再开通」会累积墓碑行 + 残留多条 deleted = 0 的重复行，
 * {@code getTenantModules} 会返回重复模块、后续「开通」到底更新哪一行也不确定。</p>
 *
 * <p><b>所以这里用显式 SQL</b>：{@code @Select} / {@code @Update} 不经过
 * MyBatis-Plus 的逻辑删除注入，能看见并复活墓碑行。两条语句都带
 * {@code @InterceptorIgnore(tenantLine = "true")} 且**显式传入 tenant_id**
 * —— 与 {@code SysModuleMapper#selectInstalledModules} 同处置：本表的 tenant_id 是
 * 「给哪个租户开通」的**数据归属**，不是「会话租户过滤条件」，平台侧必须能跨租户读写；
 * 靠会话租户隐式注入会在「平台管理员未带豁免标记」时把写入静默限定到自己的租户。
 * 注意 {@code sys_tenant_module} **不在** {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 里，
 * 不显式忽略就会被注入 {@code tenant_id = <会话租户>}。</p>
 *
 * <p>写入入口只有平台侧开通/停用接口（{@code TenantModuleController}），
 * 且该接口另有 {@code assertPlatformAdmin()} 硬校验，故本 Mapper 不会成为越权面。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Mapper
public interface TenantModuleTombstoneMapper {

    /**
     * 查某租户某模块的开通记录，<b>包含已软删（墓碑）行</b>。
     *
     * <p>{@code ORDER BY deleted ASC, id ASC LIMIT 1}：优先返回仍然有效的行（deleted = 0），
     * 其次才是最早的墓碑行 —— 历史数据里同一对 (tenant_id, module_code) 可能存在多行
     * （见类注释：无唯一索引 + 旧实现盲插），这里固定取一行作为「本对的主记录」，
     * 保证同一份数据每次结果一致（不依赖数据库返回顺序）。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant_module "
            + "WHERE tenant_id = #{tenantId} AND module_code = #{moduleCode} "
            + "ORDER BY deleted ASC, id ASC LIMIT 1")
    SysTenantModule selectAnyIncludingDeleted(@Param("tenantId") Long tenantId,
                                             @Param("moduleCode") String moduleCode);

    /**
     * 复活（或就地更新）指定的开通记录：清墓碑标记、置为正常、刷新套餐与到期时间。
     *
     * <p>按主键更新，不涉及逻辑删除字段的自动注入 ⇒ 对墓碑行有效。
     * {@code status = 0} 表示启用（见 {@code SysTenantModule#status} 注释：0-正常 1-停用）。</p>
     *
     * @return 受影响行数（0 表示 id 不存在；调用方应据此排查）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE sys_tenant_module SET deleted = 0, status = 0, module_name = #{moduleName}, "
            + "purchase_type = #{purchaseType}, expire_time = #{expireTime}, update_time = now() "
            + "WHERE id = #{id}")
    int reviveById(@Param("id") Long id,
                   @Param("moduleName") String moduleName,
                   @Param("purchaseType") String purchaseType,
                   @Param("expireTime") LocalDateTime expireTime);
}
