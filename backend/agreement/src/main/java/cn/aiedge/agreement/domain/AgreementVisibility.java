package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * 协议可见性 —— <b>全仓唯一构造处</b>（裁定⑥的防线）。
 *
 * <h3>为什么会有这么一个类</h3>
 * 协议四张表都登记在 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 里：
 * {@code agreement.tenant_id} 恒为 0（系统级），自动租户隔离在这四张表上
 * <b>不生效</b>。原因是协议天然跨租户 —— 若按会话租户自动过滤，
 * 乙方一行也查不到，一份只有一方看得见的协议既无法双签也无法共同执行。
 *
 * <h3>由此产生的代价与防线</h3>
 * "谁能看到一份协议"变成**要自己写对**的东西。若把
 * {@code party_a_tenant_id = ? OR party_b_tenant_id = ?} 散落在多个 Service 方法里，
 * 迟早有人漏写一处 ⇒ 跨租户数据泄露。
 * ⇒ 本仓纪律：**该条件只在本类构造**，Service 一律调用本类，不许自己拼。
 *
 * <p>{@code tools/verify-agreement.cjs} 在真机上钉死这条防线：
 * 会话租户不是任何一端的第三方，查不到那份协议（而该行在库里确实存在）。</p>
 */
public final class AgreementVisibility {

    private AgreementVisibility() {
    }

    /**
     * 把"本会话可见的协议"条件追加到查询条件上（分页 / 列表 / 详情 / 存在性判断都用它）。
     *
     * <p>条件是 {@code tenant_id = 0 AND (party_a_tenant_id = 会话租户 OR party_b_tenant_id = 会话租户)}。
     * 其中 {@code tenant_id = 0} 是**防御性**条件：本表约定恒为 0，若哪天有人写进了别的值，
     * 那些行会**看不见**（fail-closed）而不是被所有租户看见 —— 宁可少显示，不可多显示。</p>
     *
     * @param sessionTenantId 登录会话租户；为 null 时抛 401（不允许"无租户看到全部"）
     */
    public static LambdaQueryWrapper<Agreement> apply(LambdaQueryWrapper<Agreement> wrapper, Long sessionTenantId) {
        Long tenantId = requireSessionTenant(sessionTenantId);
        wrapper.eq(Agreement::getTenantId, 0L);
        wrapper.and(w -> w.eq(Agreement::getPartyATenantId, tenantId)
                .or()
                .eq(Agreement::getPartyBTenantId, tenantId));
        return wrapper;
    }

    /** 字符串列版本，供无法用 Lambda 的场合（如手写 SQL 的 wrapper）使用，口径与上面完全一致。 */
    public static QueryWrapper<Agreement> apply(QueryWrapper<Agreement> wrapper, Long sessionTenantId) {
        Long tenantId = requireSessionTenant(sessionTenantId);
        wrapper.eq("tenant_id", 0L);
        wrapper.and(w -> w.eq("party_a_tenant_id", tenantId).or().eq("party_b_tenant_id", tenantId));
        return wrapper;
    }

    /**
     * 「两端正好是这一对租户」的条件（**方向不敏感**）：
     * {@code (甲=A,乙=B) OR (甲=B,乙=A)}。
     *
     * <p><b>为什么它必须也放在本类里</b>：这是另一条会写 {@code party_a_tenant_id} /
     * {@code party_b_tenant_id} 的条件。若散在 {@code AgreementRuntime} 里手写，
     * 本类就不再是"唯一构造处"了 —— 裁定⑥ 要防的正是"多处各写一遍、迟早漏一处"。
     * 口径与 {@link #apply} 一致：都先钉 {@code tenant_id = 0}（fail-closed）。</p>
     *
     * <p>用途：{@code AgreementRuntime.resolve(卖方主体, 买方主体, 租户对, 业务时点)} ——
     * 调用方（订单路由 / 结算）已用会话身份定住了这对租户，本条件即"这份协议必须覆盖他们俩"。</p>
     *
     * @param tenantIdA 协议一端所属租户
     * @param tenantIdB 协议另一端所属租户；两端都不可为空（为空说明调用方根本没定住方向）
     */
    public static LambdaQueryWrapper<Agreement> applyTenantPair(LambdaQueryWrapper<Agreement> wrapper,
                                                               Long tenantIdA, Long tenantIdB) {
        if (tenantIdA == null || tenantIdB == null) {
            throw BusinessException.badRequest("必须提供协议两端所属租户，否则无法确定按哪一份协议执行");
        }
        wrapper.eq(Agreement::getTenantId, 0L);
        wrapper.and(w -> w
                .and(x -> x.eq(Agreement::getPartyATenantId, tenantIdA)
                        .eq(Agreement::getPartyBTenantId, tenantIdB))
                .or(y -> y.eq(Agreement::getPartyATenantId, tenantIdB)
                        .eq(Agreement::getPartyBTenantId, tenantIdA)));
        return wrapper;
    }

    /**
     * 内存判定：本会话能否看到这份协议（与 {@link #apply} 同口径）。
     * 供"已按 id 取回主档、再判是否越权"的场景使用。
     */
    public static boolean canSee(Agreement agreement, Long sessionTenantId) {
        if (agreement == null || sessionTenantId == null) {
            return false;
        }
        return sessionTenantId.equals(agreement.getPartyATenantId())
                || sessionTenantId.equals(agreement.getPartyBTenantId());
    }

    /**
     * 判定并抛错：看不到时一律按「协议不存在」回话（404）。
     *
     * <p>为什么是 404 而不是 403：403 等于告诉对方"这份协议存在、只是你没权限"，
     * 把<b>别人的商业关系存在性</b>泄露了出去。协议是跨租户契约，存在性本身就是敏感信息。</p>
     */
    public static void assertVisible(Agreement agreement, Long sessionTenantId) {
        if (!canSee(agreement, sessionTenantId)) {
            // 文案与"真的不存在"完全一致，不区分 —— 避免通过回包差异探测他人协议是否存在
            throw BusinessException.notFound("协议不存在或你不是本协议的任一缔约方");
        }
    }

    private static Long requireSessionTenant(Long sessionTenantId) {
        if (sessionTenantId == null) {
            throw BusinessException.unauthorized("无法确定当前登录租户，请重新登录后再操作");
        }
        return sessionTenantId;
    }
}
