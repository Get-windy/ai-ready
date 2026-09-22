package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementListConditions;
import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.enums.AgreementType;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 列表筛选条件（{@code agreementScope} / {@code agreementType}）在 Wrapper 上**实际生成的条件**。
 *
 * <p>为什么断言 Wrapper 而不是只 mock 掉：本次修的缺陷是"前端取回后本地剔除 ⇒ 分页 total
 * 与实际显示行数不一致"。修好的判据就是——范围条件必须**真的进了 SQL 条件**
 * （分页拦截器按它算 count），而不是服务层查完再筛。因此这里直接检查
 * {@link LambdaQueryWrapper#getSqlSegment()} 与 {@link LambdaQueryWrapper#getParamNameValuePairs()}。</p>
 *
 * <p>同时钉死两条不经数据库也能验的纪律：可见性条件在任何组合下都在（裁定⑥的唯一收敛点）；
 * 不传 scope 时不加任何类型条件（零行为变化）。</p>
 */
class AgreementQueryScopeTest {

    private static final Long SESSION_TENANT = 2L;

    /** 纯单元测试没有 Spring/MyBatis 环境，手工注册实体 → 列名映射，否则 lambda 取不到列名。 */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
    }

    // ══════════════ agreementScope 的三种情形 ══════════════

    @Test
    @DisplayName("agreementScope=PLATFORM ⇒ SQL 里只有 agreement_type = 'PLATFORM_SERVICE'")
    void platformScopeEqualsPlatformService() {
        LambdaQueryWrapper<Agreement> wrapper = build(query("PLATFORM", null));

        String sql = sql(wrapper);
        assertTrue(sql.contains("agreement_type"), "平台范围必须落到 SQL 条件上（否则 total 仍按全量算）: " + sql);
        assertTrue(sql.contains("agreement_type="), "平台范围应是等值条件: " + sql);
        assertFalse(sql.contains("agreement_type<>") || sql.contains("agreement_type!="),
                "平台范围不该出现不等值条件: " + sql);
        assertEquals(1, countParam(wrapper, AgreementType.PLATFORM_SERVICE.name()),
                "PLATFORM_SERVICE 应作为参数绑定一次（进 SQL，而不是内存筛选）");
    }

    @Test
    @DisplayName("agreementScope=TENANT ⇒ SQL 里是 agreement_type <> 'PLATFORM_SERVICE'（排除平台协议）")
    void tenantScopeExcludesPlatformService() {
        LambdaQueryWrapper<Agreement> wrapper = build(query("TENANT", null));

        String sql = sql(wrapper);
        assertTrue(sql.contains("agreement_type<>") || sql.contains("agreement_type!="),
                "租户范围必须用不等值条件排除平台协议，否则分页会把平台协议也算进 total: " + sql);
        assertEquals(1, countParam(wrapper, AgreementType.PLATFORM_SERVICE.name()),
                "PLATFORM_SERVICE 应作为排除参数绑定一次");
    }

    @Test
    @DisplayName("不传 agreementScope ⇒ 不加任何类型条件（与扩展前的行为逐字一致）")
    void noScopeAddsNoTypeCondition() {
        LambdaQueryWrapper<Agreement> wrapper = build(new AgreementQuery());

        String sql = sql(wrapper);
        assertFalse(sql.contains("agreement_type"), "不传范围时不许出现任何类型条件: " + sql);
        for (AgreementType t : AgreementType.values()) {
            assertEquals(0, countParam(wrapper, t.name()), "不传范围时不该绑定类型参数：" + t.name());
        }
    }

    @Test
    @DisplayName("scope 传空串 / 空白 ⇒ 与不传一致（不加类型条件）")
    void blankScopeIsSameAsAbsent() {
        for (String blank : List.of("", "   ")) {
            LambdaQueryWrapper<Agreement> wrapper = build(query(blank, null));
            assertFalse(sql(wrapper).contains("agreement_type"), "空白 scope 不应产生类型条件: [" + blank + "]");
        }
    }

    // ══════════════ scope 与 type 同时传：以 agreementType 为准 ══════════════

    @Test
    @DisplayName("scope 与 agreementType 同时传 ⇒ 以更具体的 agreementType 为准（不叠加、不报错）")
    void explicitTypeWinsOverScope() {
        LambdaQueryWrapper<Agreement> wrapper = build(query("TENANT", "DISTRIBUTION"));

        String sql = sql(wrapper);
        assertTrue(sql.contains("agreement_type="), "应保留具体类型的等值条件: " + sql);
        assertFalse(sql.contains("agreement_type<>") || sql.contains("agreement_type!="),
                "不应同时再叠加范围的不等值条件: " + sql);
        assertEquals(1, countParam(wrapper, "DISTRIBUTION"));
        assertEquals(0, countParam(wrapper, AgreementType.PLATFORM_SERVICE.name()),
                "类型优先时不再绑定 PLATFORM_SERVICE");
    }

    @Test
    @DisplayName("agreementType 大小写不敏感，落进 SQL 参数的是规范枚举名")
    void typeIsCaseInsensitive() {
        LambdaQueryWrapper<Agreement> wrapper = build(query(null, "platform_service"));
        assertEquals(1, countParam(wrapper, AgreementType.PLATFORM_SERVICE.name()),
                "小写传参应解析成规范枚举名后绑定: " + sql(wrapper));
    }

    // ══════════════ 可见性（裁定⑥）在任何组合下都在 ══════════════

    @Test
    @DisplayName("可见性条件不被绕过：任何 scope / type 组合下都在，且仍由 AgreementVisibility 生成")
    void visibilityAlwaysApplied() {
        List<AgreementQuery> queries = List.of(
                new AgreementQuery(),
                query("TENANT", null),
                query("PLATFORM", null),
                query(null, "DISTRIBUTION"),
                query("TENANT", "CONSUMER_PROMISE"));
        for (AgreementQuery q : queries) {
            LambdaQueryWrapper<Agreement> wrapper = build(q);
            String sql = sql(wrapper);
            assertTrue(sql.contains("tenant_id="), "防御性条件 tenant_id = 0 应在: " + sql);
            assertEquals(1, countParam(wrapper, 0L), "tenant_id 应绑定 0（系统级主档）: " + sql);
            assertTrue(sql.contains("(party_a_tenant_id=") && sql.contains("orparty_b_tenant_id="),
                    "两端可见性条件应在（唯一构造处生成，不在别处另写一份）: " + sql);
            assertEquals(2, countParam(wrapper, SESSION_TENANT),
                    "会话租户应作为两端可见性参数各绑定一次: " + sql);
        }
    }

    @Test
    @DisplayName("会话租户为空 ⇒ 401（不允许「无租户看到全部」）")
    void nullTenantIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementListConditions.build(new AgreementQuery(), null));
        assertEquals(401, e.getCode());
    }

    // ══════════════ 非法取值 ══════════════

    @Test
    @DisplayName("scope 取值非法 ⇒ 明确报错（中文文案列出可选值），而不是静默忽略成「不过滤」")
    void invalidScopeRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> build(query("PLATFROM", null)));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("TENANT") && e.getMessage().contains("PLATFORM"), e.getMessage());
    }

    @Test
    @DisplayName("agreementType 取值非法 ⇒ 明确报错（沿用既有文案）")
    void invalidTypeRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> build(query("TENANT", "NOT_A_TYPE")));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("协议类型"), e.getMessage());
    }

    // ══════════════ 辅助 ══════════════

    private static LambdaQueryWrapper<Agreement> build(AgreementQuery query) {
        return AgreementListConditions.build(query, SESSION_TENANT);
    }

    private static AgreementQuery query(String scope, String agreementType) {
        AgreementQuery q = new AgreementQuery();
        q.setAgreementScope(scope);
        q.setAgreementType(agreementType);
        return q;
    }

    /** 去空白 + 小写后的 SQL 片段（"agreement_type = #{...}" → "agreement_type=#{...}"），便于跨版本断言。 */
    private static String sql(LambdaQueryWrapper<Agreement> wrapper) {
        return wrapper.getSqlSegment().replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * 参数值里等于给定值的绑定次数（0 表示该值没进 SQL）。
     *
     * <p>⚠️ 必须先渲染一次 {@link LambdaQueryWrapper#getSqlSegment()}：MP 3.5.10 的列条件是把
     * {@code formatSqlMaybeWithParam} 包成 {@code ISqlSegment} 的 lambda 挂上去的，
     * <b>参数是在渲染时才写进 {@code paramNameValuePairs} 的</b>。
     * 不先渲染就读参数表会拿到空表 ⇒ 断言假失败（本仓实踩）。</p>
     */
    private static int countParam(LambdaQueryWrapper<Agreement> wrapper, Object value) {
        wrapper.getSqlSegment();
        int count = 0;
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        for (Object v : params.values()) {
            if (value.equals(v)) {
                count++;
            }
        }
        return count;
    }
}
