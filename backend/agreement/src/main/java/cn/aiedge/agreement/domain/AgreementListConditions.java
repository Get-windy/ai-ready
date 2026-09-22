package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.enums.AgreementScope;
import cn.aiedge.agreement.enums.AgreementType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 协议列表的「可见性 + 类型范围」条件组装处。
 *
 * <h3>为什么单独立一个类</h3>
 * 这两类条件都必须是<b>服务端 SQL 条件</b>，不能取回后在内存里筛：
 * 分页拦截器是按 Wrapper 里的条件算 {@code count} 的，内存筛选会让
 * "共 N 条"与实际显示行数对不上（本模块修的就是这个缺陷）。
 *
 * <h3>{@code agreementScope} 与 {@code agreementType} 同时传时以 {@code agreementType} 为准</h3>
 * 理由三条：
 * <ol>
 *   <li><b>更具体的一方说话</b>：范围（平台级 / 租户级）只分两档，具体类型是用户在下拉里的显式选择，
 *       两者冲突时按更具体的那个走，符合直觉；</li>
 *   <li><b>不打断操作</b>：两个入口共用同一个列表页，用户切完入口还会手动改类型筛选。
 *       若改成"同时传就报错"，用户只是想看一眼某种协议就会撞 400，把可自解的问题变成故障提示；</li>
 *   <li><b>向后兼容</b>：只传 {@code agreementType} 的既有调用方语义一字不变（新参数是纯扩展）。</li>
 * </ol>
 * ⚠️ 以 {@code agreementType} 为准<b>不会放松可见性</b>：{@link AgreementVisibility} 的条件始终先追加，
 * 类型条件只是在此之上做收窄或放宽类型维度，跨租户那一层防线不受影响。
 *
 * <p><b>不传 {@code agreementScope}、也不传 {@code agreementType}</b> ⇒ 不追加任何类型条件，
 * 行为与本次扩展之前<b>逐字一致</b>（零行为变化）。</p>
 *
 * <p>状态 / 关键字 / 排序不属于"必须收敛"的条件，仍在服务层追加。</p>
 */
public final class AgreementListConditions {

    private AgreementListConditions() {
    }

    /**
     * 组装列表查询条件（顺序即 SQL 里的 AND 顺序，condition 之间互相独立）。
     *
     * @param query           列表查询条件（{@code agreementScope} / {@code agreementType} 可空）
     * @param sessionTenantId 登录会话租户；为 null 时由 {@link AgreementVisibility} 抛 401
     */
    public static LambdaQueryWrapper<Agreement> build(AgreementQuery query, Long sessionTenantId) {
        LambdaQueryWrapper<Agreement> wrapper = new LambdaQueryWrapper<>();

        // ① 可见性：**唯一构造处**（裁定⑥）。四张协议表不参与租户拦截器，
        //    这里是唯一的防线；本类不另写 party_a_tenant_id / party_b_tenant_id 条件。
        AgreementVisibility.apply(wrapper, sessionTenantId);

        // ② 类型：具体类型优先于范围（理由见类注释）
        applyTypeScope(wrapper, query);
        return wrapper;
    }

    /**
     * **平台合规抽查读**的列表条件（DOMAIN-MODEL §13.9）。
     *
     * <p>与 {@link #build} 的差别**只有可见性那一句**：换成
     * {@link AgreementVisibility#applyPlatformCompliance}（不限制两端之一）。
     * 类型条件复用同一个 {@link #applyTypeScope} —— 两处若各写一遍，
     * 迟早出现"租户侧能筛平台协议、平台侧筛不了"这类不对称缺陷。</p>
     *
     * <p>⚠️ 只能被 `agreement:platform:compliance:read` 保护的端点调用（见该方法的注释）。</p>
     */
    public static LambdaQueryWrapper<Agreement> buildForPlatformCompliance(AgreementQuery query) {
        LambdaQueryWrapper<Agreement> wrapper = new LambdaQueryWrapper<>();
        AgreementVisibility.applyPlatformCompliance(wrapper);
        applyTypeScope(wrapper, query);
        return wrapper;
    }

    /** 类型范围条件（两种入口共用，别各写一遍）。 */
    private static void applyTypeScope(LambdaQueryWrapper<Agreement> wrapper, AgreementQuery query) {
        String typeName = trimToNull(query.getAgreementType());
        if (typeName != null) {
            AgreementType type = AgreementType.parse(typeName);
            wrapper.eq(Agreement::getAgreementType, type.name());
            return;
        }
        AgreementScope scope = AgreementScope.parse(query.getAgreementScope());
        if (scope == AgreementScope.PLATFORM) {
            // 平台级 = 只有 PLATFORM_SERVICE
            wrapper.eq(Agreement::getAgreementType, AgreementType.PLATFORM_SERVICE.name());
        } else if (scope == AgreementScope.TENANT) {
            // 租户级 = 排除 PLATFORM_SERVICE（代销 / 购销框架 / 消费者单方承诺）
            wrapper.ne(Agreement::getAgreementType, AgreementType.PLATFORM_SERVICE.name());
        }
        // scope == null ⇒ 不加任何类型条件（历史行为不变）
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
