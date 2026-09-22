package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementTemplate;
import cn.aiedge.agreement.enums.AgreementTemplateScope;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 契约模板的**可见性与可管理性** —— 全仓唯一构造处（与 {@link AgreementVisibility} 同一纪律）。
 *
 * <h3>为什么模板表也不能靠租户拦截器</h3>
 * {@code agreement_template.tenant_id} 是"**数据归属**"（这份模板是谁的），
 * 而平台模板的归属位是 0、租户模板是各自的租户 —— 两级混在一张表里。
 * 若交给自动注入，要么平台模板对所有租户消失（都变成"看不到模板"），
 * 要么租户模板泄给别的租户。两种都错，所以条件必须显式写、且只写在这一处。
 *
 * <h3>读口径（用户 2026-09-22 明确要求）</h3>
 * <ul>
 *   <li><b>平台可读全部</b>（含租户模板）：这是**合规抽查读权限**，目的很实在 ——
 *       避免平台里出现非法交易的约定。权限码 {@code agreement:platform:template:read}
 *       （归「系统」模块，只开给系统租户）。</li>
 *   <li><b>租户只能读</b>「平台模板 + 自己的模板」：既看得到平台给的起点，
 *       又看不到别家租户的商业条款（那本身就是同业敏感信息）。</li>
 * </ul>
 *
 * <h3>管理口径（写）——与读**不对称**，这是有意的</h3>
 * <ul>
 *   <li>平台模板：只有平台侧能建 / 改 / 删（码 {@code agreement:platform:template:manage}）。</li>
 *   <li>租户模板：只有**它自己的租户**能建 / 改 / 删。
 *       ⚠️ 平台侧虽然能**读**（合规抽查），但**不能改**租户模板 ——
 *       合规抽查是"看"，一旦能改就变成平台替租户定商业条款（违反㉜ 的口径）。</li>
 * </ul>
 */
public final class AgreementTemplateVisibility {

    private AgreementTemplateVisibility() {
    }

    /**
     * 把"本会话可见的模板"条件追加到查询上（列表 / 详情 / 存在性判断都用它）。
     *
     * @param platformSide 是否平台侧（超管 / 系统租户）：true ⇒ 不加归属条件（可读全部）
     */
    public static LambdaQueryWrapper<AgreementTemplate> apply(LambdaQueryWrapper<AgreementTemplate> wrapper,
                                                             Long sessionTenantId, boolean platformSide) {
        if (platformSide) {
            // 合规抽查读：平台可读全部模板（含租户模板）。⚠️ 只放开**读**，写权限另行判定。
            return wrapper;
        }
        Long tenantId = requireSessionTenant(sessionTenantId);
        wrapper.and(w -> w
                .eq(AgreementTemplate::getScope, AgreementTemplateScope.PLATFORM.name())
                .or(x -> x.eq(AgreementTemplate::getScope, AgreementTemplateScope.TENANT.name())
                        .eq(AgreementTemplate::getTenantId, tenantId)));
        return wrapper;
    }

    /** 内存判定：本会话能否读到这份模板（与 {@link #apply} 同口径）。 */
    public static boolean canSee(AgreementTemplate template, Long sessionTenantId, boolean platformSide) {
        if (template == null) {
            return false;
        }
        if (platformSide) {
            return true;
        }
        if (sessionTenantId == null) {
            return false;
        }
        if (AgreementTemplateScope.PLATFORM.name().equalsIgnoreCase(template.getScope())) {
            return true;
        }
        return sessionTenantId.equals(template.getTenantId());
    }

    /**
     * 判定并抛错：读不到时一律按「模板不存在」回话（404）。
     *
     * <p>与协议主档同口径：403 等于告诉对方"这份模板存在、只是你没权限"，
     * 把别家租户的商业条款存在性泄露了出去。</p>
     */
    public static void assertVisible(AgreementTemplate template, Long sessionTenantId, boolean platformSide) {
        if (!canSee(template, sessionTenantId, platformSide)) {
            throw BusinessException.notFound("模板不存在或不属于你所在的租户");
        }
    }

    /**
     * 判定并抛错：本会话能否**建 / 改 / 删**这份模板（写口径见类注释，与读不对称）。
     */
    public static void assertManageable(AgreementTemplate template, Long sessionTenantId, boolean platformSide) {
        assertVisible(template, sessionTenantId, platformSide);
        boolean platformTemplate = AgreementTemplateScope.PLATFORM.name().equalsIgnoreCase(template.getScope());
        if (platformTemplate && !platformSide) {
            throw BusinessException.forbidden("平台模板由平台统一维护，租户不能修改；"
                    + "如需按自己的业务调整，请「另存为本租户模板」后修改");
        }
        if (!platformTemplate && (platformSide || !template.getTenantId().equals(sessionTenantId))) {
            // 平台侧能读（合规抽查）但不能改：一旦能改就变成平台替租户定商业条款
            throw BusinessException.forbidden("租户模板只能由该租户自己维护"
                    + "（平台可合规抽查查看，但不代为修改）");
        }
    }

    /**
     * 新建模板时的级别与归属校验：平台模板只有平台侧能建，且归属位必须是 0；
     * 租户模板归属位必须是<b>本租户</b>（不接受前端传归属，否则可以把模板挂到别人名下）。
     *
     * @return 该写入的 tenant_id
     */
    public static long resolveOwnerTenantId(AgreementTemplateScope scope, Long sessionTenantId, boolean platformSide) {
        if (scope == AgreementTemplateScope.PLATFORM) {
            if (!platformSide) {
                throw BusinessException.forbidden("只有平台侧能新建平台模板");
            }
            // 平台模板是系统级参考数据：归属位必须显式为 0（不写会被填充成会话租户）
            return 0L;
        }
        return requireSessionTenant(sessionTenantId);
    }

    private static Long requireSessionTenant(Long sessionTenantId) {
        if (sessionTenantId == null) {
            throw BusinessException.unauthorized("无法确定当前登录租户，请重新登录后再操作");
        }
        return sessionTenantId;
    }
}
