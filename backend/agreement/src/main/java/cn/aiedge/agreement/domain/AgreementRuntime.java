package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.FulfillmentMode;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementSettingDefMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 生效设定的**唯一读取入口**（DOMAIN-MODEL §13.3）。
 *
 * <pre>
 * AgreementRuntime.resolve(卖方主体, 买方主体, 租户对, 业务时点) → 该时点生效那一版的设定集
 * </pre>
 *
 * <h3>三条不可动摇的口径</h3>
 * <ol>
 *   <li><b>必须带业务时点</b>：在版本集合里找"该时点生效的那一版"（㉛：下单时刻生效的那一版）。
 *       {@code businessTime} 为 {@code null} 一律<b>拒绝</b>，不给"那就用当前版本吧"的方便 ——
 *       那等于允许改协议篡改已发生交易的口径。区间与挑选规则见 {@link AgreementEffectiveVersion}。</li>
 *   <li><b>未约定 ⇒ 返回"未约定"，绝不回落到默认值</b>：取值用 {@link AgreementSettingValue}
 *       表达三态（已约定 / 未约定 / 该字段未定义），不许用 {@code null} 混同。
 *       严格取值走 {@link AgreementResolvedSettings#require(String, String)}：
 *       未约定时抛中文业务异常（例：「本协议未约定账期，无法生成应收到期日」）。</li>
 *   <li><b>绝不读模板</b>：模板项只是发起时的预填，不是"已约定"（㉜ / §13.9）。
 *       详见下方"模板不是默认值"。</li>
 * </ol>
 *
 * <h3>⚠️⚠️ 模板不是默认值（㉜ 的防线，改本类之前先读这一段）</h3>
 * 契约模板（{@code agreement_template*} 四张表）是**显式选择的起点**，
 * 不是"自动套用的默认值"。因此：
 * <ul>
 *   <li>本类<b>刻意不持有</b>任何模板相关的 Mapper（构造参数里就没有）——
 *       结构上不可能去读模板，请保持这一点；
 *       单测 {@code AgreementRuntimeTest} 用**反射**钉死"本类不依赖任何模板表"，
 *       并在调用路径上 {@code verify(templateMapper, never())} 再钉一次。</li>
 *   <li>模板项只在"从模板发起契约"时**预填**到新草稿版本上，仍需双方在那一版上确认与双签。</li>
 *   <li>"模板里有 ⇒ 视为已约定"是**绝不允许**的 —— 那等于平台替双方定了商业条款。</li>
 * </ul>
 *
 * <h3>可见性</h3>
 * 本类查协议主档时用 {@link AgreementVisibility#applyTenantPair}（与裁定⑥ 的
 * "可见性条件只在一处构造"同一纪律）：<b>两端租户必须与调用方声称的一致</b>。
 * 也就是说调用方（订单路由 / 结算）已经用会话身份定住了两端的租户对，
 * 本类不再自己拼任何 {@code party_a_tenant_id = ? OR party_b_tenant_id = ?} 条件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgreementRuntime {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 「履约方式集合」的字段编码：它是**集合**语义，值落在 {@code agreement_fulfillment_mode}，
     * 但仍在字段元数据里登记一行（这样界面能显示"这一项影响订单路由"）。
     */
    public static final String FULFILLMENT_MODES_KEY = "FULFILLMENT_MODES";

    /**
     * **消费方白名单**（§13.2 的"字段 → 消费方"表，落成可断言的枚举）。
     *
     * <p>{@code agreement_setting_def.consumer_point} 只能取这里的名字，含义是
     * "<b>这个字段被谁消费</b>"。为什么它是硬要求而不是注释：
     * 本仓最贵的历史包袱恰恰是"配置界面能勾、勾了不生效"（零消费的开关、
     * 配了不生效的数据范围、只填一半的 api_path）。<b>没有消费方的字段不许进设定版。</b></p>
     *
     * <p>界面拿它显示"这一项会影响什么"，用户改一个字段前就能看到后果。</p>
     */
    public enum ConsumerPoint {

        ORDER_ROUTING("订单路由", "下单时按这份协议确定由谁履约、用哪种履约方式"),
        SHIPMENT_GEN("发货单生成", "生成发货单时决定从哪发出、运费由谁承担"),
        STOCK_DEDUCT("库存扣减点", "决定下单扣谁的库存、直发时是否不占本仓库存"),
        AVAILABLE_QTY("可售量校验", "决定这个商品在店里最多能卖多少（额度约束）"),
        PRICING("定价引擎", "决定按哪个价卖、最低不能低于多少"),
        AR_DUE_DATE("应收应付到期日", "决定这笔账什么时候到期（账期与方向）"),
        SETTLEMENT_SPLIT("结算分账", "决定佣金/抽成怎么分、按什么周期结"),
        INVOICING("发票生成", "决定由谁开票给谁"),
        ORDER_RISK("下单风控", "决定授权区域与信用额度，超限拦单"),
        CANCEL_FLOW("取消流程", "取消时由谁承担、收多少取消费"),
        RETURN_FLOW("退货流程", "退货走哪条路径、退货运费谁出"),
        CLAIM_FLOW("理赔流程", "质量责任归属，用于理赔定责口径"),
        DEPOSIT_FORFEIT("保证金扣罚", "违约情形下扣罚比例与上限（依据来自双方设定）");

        private final String label;

        private final String semantics;

        ConsumerPoint(String label, String semantics) {
            this.label = label;
            this.semantics = semantics;
        }

        public String getLabel() {
            return label;
        }

        public String getSemantics() {
            return semantics;
        }

        public static ConsumerPoint of(String name) {
            if (name == null) {
                return null;
            }
            String n = name.trim();
            for (ConsumerPoint p : values()) {
                if (p.name().equalsIgnoreCase(n)) {
                    return p;
                }
            }
            return null;
        }

        /** 中文名；取不到时返回原值（保证报错文案里总有个能搜的东西）。 */
        public static String labelOf(String name) {
            ConsumerPoint p = of(name);
            return p == null ? (name == null ? "" : name) : p.label;
        }
    }

    private final AgreementMapper agreementMapper;
    private final AgreementVersionMapper versionMapper;
    private final AgreementSettingMapper settingMapper;
    private final AgreementSettingDefMapper settingDefMapper;
    private final AgreementFulfillmentModeMapper fulfillmentModeMapper;

    // ══════════════════════ 解析入口 ══════════════════════

    /**
     * 按**主体的那一对**解析：卖方主体 / 卖方租户 / 买方主体 / 买方租户 + 业务时点。
     *
     * <p>方向不敏感：{@code (甲,乙)} 与 {@code (乙,甲)} 都能命中同一份协议 ——
     * 协议两端是**对称**的（裁定②），"谁是卖方"由单据决定，不由协议存法决定。
     * 命中多份生效协议时**拒绝执行并明确报错**（同一对主体之间不许并存两份生效协议，
     * 真出现说明数据异常，此时猜一份执行比报错危险得多）。</p>
     *
     * @param businessTime **业务时点**（下单/发货/结算发生的时刻），不可为空
     */
    public AgreementResolvedSettings resolve(Long partyAId, Long partyATenantId,
                                             Long partyBId, Long partyBTenantId,
                                             LocalDateTime businessTime) {
        requireBusinessTime(businessTime);
        List<AgreementSettingDef> defs = definitions();

        List<Agreement> matched = new ArrayList<>();
        for (Agreement a : activeAgreementsOfTenantPair(partyATenantId, partyBTenantId)) {
            if (matchesParties(a, partyAId, partyBId)) {
                matched.add(a);
            }
        }
        String pairText = "租户 " + partyATenantId + " 与 租户 " + partyBTenantId;
        if (matched.isEmpty()) {
            return AgreementResolvedSettings.noEffectiveVersion(
                    "没有找到覆盖" + pairText + "的生效协议（可能尚未签署生效，或已终止）", businessTime, defs);
        }
        if (matched.size() > 1) {
            throw BusinessException.badRequest("在" + pairText + "之间找到了 " + matched.size()
                    + " 份生效协议，无法判断按哪一份执行；请指明具体协议后再操作（协议编号："
                    + matched.stream().map(Agreement::getAgreementNo).reduce((x, y) -> x + "、" + y).orElse("") + "）");
        }
        return resolveFor(matched.get(0), businessTime, defs);
    }

    /** 只按**租户对**解析（用于不指定具体主体、或乙方是"不特定消费者"的场景）。 */
    public AgreementResolvedSettings resolveByTenantPair(Long tenantIdA, Long tenantIdB, LocalDateTime businessTime) {
        return resolve(null, tenantIdA, null, tenantIdB, businessTime);
    }

    /** 已知道是哪一份协议时按它解析（例如协议详情页/调试用；执行链路请用带主体对的重载）。 */
    public AgreementResolvedSettings resolveByAgreement(Long agreementId, LocalDateTime businessTime) {
        requireBusinessTime(businessTime);
        List<AgreementSettingDef> defs = definitions();
        if (agreementId == null) {
            throw BusinessException.badRequest("请提供协议 ID");
        }
        Agreement agreement = agreementMapper.selectById(agreementId);
        if (agreement == null) {
            return AgreementResolvedSettings.noEffectiveVersion("协议不存在或已删除", businessTime, defs);
        }
        return resolveFor(agreement, businessTime, defs);
    }

    /** 该协议**当前**生效那一版的设定（展示用，等价于"业务时点 = 现在"）。执行链路请显式传业务时点。 */
    public AgreementResolvedSettings resolveCurrent(Long agreementId) {
        return resolveByAgreement(agreementId, LocalDateTime.now());
    }

    // ══════════════════════ 履约方式集合 ══════════════════════

    /**
     * 某版本约定的**履约方式集合**（可多行并存：同城直发 + 异地中转同时在）。
     *
     * <p>订单路由问的第一个问题就是"这份协议允许哪些履约方式"。
     * 空集 = 这一版没约定履约方式，<b>不等于"随便用哪种"</b> —— 调用方应据此拦下并报错。</p>
     */
    public Set<FulfillmentMode> fulfillmentModes(Long versionId) {
        Set<FulfillmentMode> modes = new LinkedHashSet<>();
        if (versionId == null) {
            return modes;
        }
        List<AgreementFulfillmentMode> rows = fulfillmentModeMapper.selectList(
                new LambdaQueryWrapper<AgreementFulfillmentMode>()
                        .eq(AgreementFulfillmentMode::getVersionId, versionId)
                        .orderByAsc(AgreementFulfillmentMode::getSort));
        for (AgreementFulfillmentMode row : rows) {
            FulfillmentMode mode = FulfillmentMode.of(row.getMode());
            if (mode == null) {
                // 库里有枚举外的取值：跳过但**必须留痕**（不静默），否则就是"配了不生效"的翻版
                log.warn("协议履约方式出现未知取值，已跳过: versionId={}, mode={}", versionId, row.getMode());
                continue;
            }
            modes.add(mode);
        }
        return modes;
    }

    /** 平台字段元数据（**含停用行**：停用只影响以后新签，历史版本里仍要能解析出已约定的值）。 */
    public List<AgreementSettingDef> definitions() {
        return settingDefMapper.selectList(new LambdaQueryWrapper<AgreementSettingDef>()
                .orderByAsc(AgreementSettingDef::getSort)
                .orderByAsc(AgreementSettingDef::getSettingKey));
    }

    /**
     * 某版本设定集的**三态视图**（含"未约定/未定义"的项）—— 详情页与就绪校验共用这一份装配逻辑。
     *
     * <p>不按业务时点挑选版本（调用方已给定版本），因此它回答的是
     * "这一版约定了什么"，不是"某个时点按哪一版"。执行链路请用 {@link #resolve}。</p>
     */
    public Map<String, AgreementSettingValue> valuesOf(Long versionId) {
        return assembleValues(versionId, definitions(), fulfillmentModes(versionId));
    }

    /**
     * 取某版本里"已约定"的那些设定项（**不含**未约定的项）。
     *
     * <p>给"回执/就绪校验/详情展示"用；执行链路请用 {@link #resolve}，
     * 那样才会带上业务时点与三态。</p>
     */
    public List<AgreementSetting> agreedSettings(Long versionId) {
        if (versionId == null) {
            return List.of();
        }
        return settingMapper.selectList(new LambdaQueryWrapper<AgreementSetting>()
                .eq(AgreementSetting::getVersionId, versionId)
                .orderByAsc(AgreementSetting::getSettingKey));
    }

    // ══════════════════════ 内部 ══════════════════════

    /** 两端租户必须与调用方声称的一致，否则一行也查不到（fail-closed）。 */
    private List<Agreement> activeAgreementsOfTenantPair(Long tenantIdA, Long tenantIdB) {
        LambdaQueryWrapper<Agreement> wrapper = new LambdaQueryWrapper<>();
        AgreementVisibility.applyTenantPair(wrapper, tenantIdA, tenantIdB);
        // 只有"生效中"的主档才产生执行口径：终止/暂停的协议不自动执行任何东西
        wrapper.eq(Agreement::getStatus, AgreementStatus.ACTIVE.getCode());
        return agreementMapper.selectList(wrapper);
    }

    private AgreementResolvedSettings resolveFor(Agreement agreement, LocalDateTime businessTime,
                                                 List<AgreementSettingDef> defs) {
        List<AgreementVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getAgreementId, agreement.getId()));
        Optional<AgreementVersion> picked = AgreementEffectiveVersion.pick(versions, businessTime);
        if (picked.isEmpty()) {
            String reason = "协议「" + agreement.getAgreementNo() + "」在 "
                    + businessTime.format(TS) + " 这个时点没有生效版本（可能尚未生效、已终止、或该时点不在有效期区间内）";
            return AgreementResolvedSettings.noEffectiveVersion(reason, businessTime, defs);
        }
        AgreementVersion version = picked.get();
        Set<FulfillmentMode> modes = fulfillmentModes(version.getId());
        Map<String, AgreementSettingValue> values = assembleValues(version.getId(), defs, modes);
        if (modes.isEmpty()) {
            log.info("协议该版本未约定履约方式（订单路由应据此拦下）: agreementId={}, versionId={}, 业务时点={}",
                    agreement.getId(), version.getId(), businessTime.format(TS));
        }
        return new AgreementResolvedSettings(true, null, agreement.getId(), version.getId(),
                version.getVersionNo(), version.getEffectiveFrom(), version.getEffectiveTo(),
                businessTime, values, modes, defs);
    }

    /**
     * 把"本版存过的行"与"平台字段元数据"合成**三态**视图。
     *
     * <ul>
     *   <li>元数据里有、本版没有行 ⇒ <b>未约定</b>（UNDECLARED）；</li>
     *   <li>元数据里有、本版有行 ⇒ <b>已约定</b>（AGREED）；</li>
     *   <li>元数据里没有、本版却存过值（字段被下架/改名）⇒ <b>未定义</b>（UNDEFINED）——
     *       这是配置问题，不能当成"双方没约定"，否则排查方向会跑偏。</li>
     * </ul>
     *
     * <p><b>「履约方式集合」是唯一特例</b>：它是**集合**（一版多行并存，§13.10），
     * 值存在 {@code agreement_fulfillment_mode} 而不是 {@code agreement_setting}。
     * 为了让界面在同一张表里就能看到它到底"约定了没有"，这里把那份数据回填进来
     * （已约定 = 逗号分隔的方式编码；空集 = 未约定）。<b>唯一事实来源仍是履约方式表。</b></p>
     */
    private Map<String, AgreementSettingValue> assembleValues(Long versionId, List<AgreementSettingDef> defs,
                                                              Set<FulfillmentMode> modes) {
        Map<String, AgreementSetting> rowsByKey = new LinkedHashMap<>();
        for (AgreementSetting row : agreedSettings(versionId)) {
            if (row.getSettingKey() != null) {
                rowsByKey.put(row.getSettingKey(), row);
            }
        }
        Map<String, AgreementSettingValue> values = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        AgreementSettingDef modesDef = null;
        for (AgreementSettingDef def : defs) {
            if (def.getSettingKey() == null) {
                continue;
            }
            if (FULFILLMENT_MODES_KEY.equals(def.getSettingKey())) {
                modesDef = def;
                continue;
            }
            seen.add(def.getSettingKey());
            AgreementSetting row = rowsByKey.get(def.getSettingKey());
            values.put(def.getSettingKey(),
                    row == null ? AgreementSettingValue.undeclared(def) : AgreementSettingValue.agreed(def, row));
        }
        for (AgreementSetting row : rowsByKey.values()) {
            if (!seen.contains(row.getSettingKey())) {
                values.put(row.getSettingKey(), AgreementSettingValue.undefined(row.getSettingKey()));
            }
        }
        if (modesDef != null) {
            Set<FulfillmentMode> effective = modes == null ? Set.of() : modes;
            values.put(FULFILLMENT_MODES_KEY, effective.isEmpty()
                    ? AgreementSettingValue.undeclared(modesDef)
                    : AgreementSettingValue.agreedText(modesDef,
                            effective.stream().map(Enum::name).reduce((x, y) -> x + "," + y).orElse("")));
        }
        return values;
    }

    /** 主体对匹配（方向不敏感；消费者单方承诺的乙方为空 ⇒ 视为通配）。 */
    private static boolean matchesParties(Agreement a, Long p1, Long p2) {
        if (p1 == null && p2 == null) {
            return true;
        }
        if (p1 == null || p2 == null) {
            Long only = p1 == null ? p2 : p1;
            return only.equals(a.getPartyAId()) || only.equals(a.getPartyBId());
        }
        boolean direct = p1.equals(a.getPartyAId()) && p2.equals(a.getPartyBId());
        boolean reversed = p1.equals(a.getPartyBId()) && p2.equals(a.getPartyAId());
        boolean consumerWildcard = a.getPartyBId() == null && p1.equals(a.getPartyAId());
        return direct || reversed || consumerWildcard;
    }

    /**
     * ⚠️ 业务时点不可为空：为空就等于"读当前版本"，而"读当前版本"正是
     * ㉛ 要防的那件事（改协议会篡改已发生交易的口径）。
     * 这里刻意**不给**"为空就当现在"的方便。
     */
    private static void requireBusinessTime(LocalDateTime businessTime) {
        if (businessTime == null) {
            throw BusinessException.badRequest("必须提供业务时点（下单/发货/结算发生的时刻），"
                    + "否则无法确定按哪一版协议执行。注意：不允许按「当前版本」取数 —— 那会让改协议篡改已发生交易的口径");
        }
    }
}
