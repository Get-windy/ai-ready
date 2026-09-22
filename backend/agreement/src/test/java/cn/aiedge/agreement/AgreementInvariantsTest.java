package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementInvariants;
import cn.aiedge.agreement.domain.AgreementPartySide;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementVisibility;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议状态机与三条不变量的单元测试（**不依赖运行实例 / 数据库 / Spring**）。
 *
 * <p>覆盖的是 DOMAIN-MODEL §十二 12.2 那三条不变量与 §3.4.4d1/d2 的口径：
 * 已生效版本只读 · 双签缺一不可 · 必填条款没选完不许生效 · 未约定不回落到默认值。</p>
 */
class AgreementInvariantsTest {

    // ══════════════════ 不变量 1：已生效版本只读 ══════════════════

    @Test
    @DisplayName("已生效（ACTIVE）版本的条款与快照不可修改 —— 改协议只能新建草稿版本")
    void activeVersionIsImmutable() {
        AgreementVersion active = version(AgreementVersionStatus.ACTIVE.getCode());
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertVersionMutable(active));
        assertTrue(e.getMessage().contains("已生效"), "文案应说明已生效不可改：" + e.getMessage());

        // 草稿可写
        AgreementInvariants.assertVersionMutable(version(AgreementVersionStatus.DRAFT.getCode()));
        // 被取代 / 被否决的历史版本同样不可写
        assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertVersionMutable(version(AgreementVersionStatus.SUPERSEDED.getCode())));
        assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertVersionMutable(version(AgreementVersionStatus.REJECTED.getCode())));
    }

    // ══════════════════ 不变量 2：双签缺一不可 ══════════════════

    @Test
    @DisplayName("只有一方确认时不许置生效（直接对应「不能单方改协议」）")
    void singleSignatureIsNotEnough() {
        AgreementVersion v = version(AgreementVersionStatus.DRAFT.getCode());
        v.setPartyAConfirmedBy(9L);
        v.setPartyAConfirmedAt(LocalDateTime.now());
        v.setPartyASignHash("hash-a");

        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertBothSidesSigned(v));
        assertTrue(e.getMessage().contains("乙方"), "文案应指出缺哪一方（乙方）：" + e.getMessage());
        assertTrue(e.getMessage().contains("双方"), "文案应说明协议必须双方都确认：" + e.getMessage());
    }

    @Test
    @DisplayName("双方确认痕迹齐全才放行；只有确认人没有时间/哈希也不算确认")
    void bothSignaturesRequired() {
        AgreementVersion ok = version(AgreementVersionStatus.DRAFT.getCode());
        ok.setPartyAConfirmedBy(9L);
        ok.setPartyAConfirmedAt(LocalDateTime.now());
        ok.setPartyASignHash("hash-a");
        ok.setPartyBConfirmedBy(8L);
        ok.setPartyBConfirmedAt(LocalDateTime.now());
        ok.setPartyBSignHash("hash-b");
        AgreementInvariants.assertBothSidesSigned(ok);

        // 缺哈希 = 没有内容存证 ⇒ 不算确认
        AgreementVersion missingHash = version(AgreementVersionStatus.DRAFT.getCode());
        missingHash.setPartyAConfirmedBy(9L);
        missingHash.setPartyAConfirmedAt(LocalDateTime.now());
        missingHash.setPartyBConfirmedBy(8L);
        missingHash.setPartyBConfirmedAt(LocalDateTime.now());
        missingHash.setPartyBSignHash("hash-b");
        assertThrows(BusinessException.class, () -> AgreementInvariants.assertBothSidesSigned(missingHash));
    }

    // ══════════════════ 不变量 3：必填条款没选完不许生效 ══════════════════

    @Test
    @DisplayName("必填条款没选完，置生效被拒且文案指出缺的是哪一项")
    void missingRequiredTermBlocksActivation() {
        List<AgreementTermOption> options = List.of(
                option("RETURN_FREIGHT", "TO_SUPPLIER", "退回运费由供货方承担", true),
                option("CANCEL_POLICY", "BOTH_AGREE", "取消需双方同意", false));
        Set<String> required = AgreementInvariants.requiredTermCodes(options);
        assertEquals(Set.of("RETURN_FREIGHT"), required);

        // 一项都没选
        List<String> missing = AgreementInvariants.missingRequiredTermCodes(List.of(), required);
        assertEquals(List.of("RETURN_FREIGHT"), missing);

        AgreementVersion v = fullySignedVersion();
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertActivatable(v, missing, List.of()));
        assertTrue(e.getMessage().contains("RETURN_FREIGHT"), e.getMessage());
        assertTrue(e.getMessage().contains("不能生效"), e.getMessage());
        // 平台不提供默认值的口径必须写在文案里
        assertTrue(e.getMessage().contains("默认值"), e.getMessage());
    }

    @Test
    @DisplayName("必填条款选齐 + 双签齐 ⇒ 放行（生效前置条件成立）")
    void allRequiredSelectedAndSignedPasses() {
        AgreementTerm selected = term("RETURN_FREIGHT", "TO_SUPPLIER", null);
        Set<String> required = Set.of("RETURN_FREIGHT");
        assertTrue(AgreementInvariants.missingRequiredTermCodes(List.of(selected), required).isEmpty());
        AgreementInvariants.assertActivatable(fullySignedVersion(), List.of(), List.of());
    }

    @Test
    @DisplayName("已选条款需要参数却没填 ⇒ 置生效被拒，并说明还差哪个参数")
    void missingParamBlocksActivation() {
        AgreementTermOption opt = option("PROFIT_SHARE", "SHARE", "按比例分账", false);
        opt.setNeedsParam("分账比例");
        AgreementTerm term = term("PROFIT_SHARE", "SHARE", null);
        List<String> missing = AgreementInvariants.missingParams(List.of(term), List.of(opt));
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).contains("分账比例"), missing.get(0));

        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertActivatable(fullySignedVersion(), List.of(), missing));
        assertTrue(e.getMessage().contains("分账比例"), e.getMessage());
    }

    // ══════════════════ 拒绝：只有草稿能被否决 ══════════════════

    @Test
    @DisplayName("只有「待双方确认」的版本能被否决；已生效版本不能被否决")
    void onlyDraftCanBeRejected() {
        AgreementInvariants.assertRejectable(version(AgreementVersionStatus.DRAFT.getCode()));
        assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertRejectable(version(AgreementVersionStatus.ACTIVE.getCode())));
    }

    // ══════════════════ 「未约定」不回落到平台默认值（§3.4.4d1） ══════════════════

    @Test
    @DisplayName("生效版本里缺席的条款 ⇒ 回答「未约定」（空），绝不回落到任何平台默认值")
    void undeclaredTermAnswersEmpty() {
        List<AgreementTerm> terms = List.of(term("CANCEL_POLICY", "BOTH_AGREE", null));
        assertTrue(AgreementInvariants.findAgreedTerm(terms, "RETURN_FREIGHT").isEmpty(),
                "未约定必须如实回答未约定，不能给默认值");
        Optional<AgreementTerm> found = AgreementInvariants.findAgreedTerm(terms, "CANCEL_POLICY");
        assertTrue(found.isPresent());
        assertEquals("BOTH_AGREE", found.get().getOptionCode());
        // 只有类别没有选项的行不算"已约定"
        assertTrue(AgreementInvariants.findAgreedTerm(List.of(term("RETURN_FREIGHT", null, null)),
                "RETURN_FREIGHT").isEmpty());
    }

    @Test
    @DisplayName("条款必须来自字典且启用且未被判定违法，否则拒绝写入")
    void termSelectionMustComeFromDictionary() {
        List<AgreementTermOption> options = List.of(option("RETURN_FREIGHT", "TO_SUPPLIER", "退回运费由供货方承担", true));
        // 不在字典里
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertTermsSelectable(
                        List.of(term("RETURN_FREIGHT", "NOT_EXIST", null)), options));
        assertTrue(e1.getMessage().contains("字典"), e1.getMessage());

        // 已停用
        AgreementTermOption disabled = option("RETURN_FREIGHT", "TO_SUPPLIER", "退回运费由供货方承担", true);
        disabled.setStatus(0);
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertTermsSelectable(
                        List.of(term("RETURN_FREIGHT", "TO_SUPPLIER", null)), List.of(disabled)));
        assertTrue(e2.getMessage().contains("停用"), e2.getMessage());

        // 法务判定违法（《消法》26 条 / 《民法典》496~498 条）
        AgreementTermOption illegal = option("RETURN_POLICY", "NO_RETURN", "不支持七日无理由退货", true);
        illegal.setLegalReviewStatus(AgreementInvariants.LEGAL_REJECTED);
        BusinessException e3 = assertThrows(BusinessException.class,
                () -> AgreementInvariants.assertTermsSelectable(
                        List.of(term("RETURN_POLICY", "NO_RETURN", null)), List.of(illegal)));
        assertTrue(e3.getMessage().contains("法务"), e3.getMessage());
    }

    // ══════════════════ 两端对称 + 可见性 ══════════════════

    @Test
    @DisplayName("本方由会话租户与两端 tenant 比对得出，前端无权声明自己是哪一方")
    void sideIsResolvedFromSessionTenant() {
        Agreement a = agreement(2L, 3L);
        assertEquals(AgreementPartySide.A, AgreementPartySide.of(a, 2L));
        assertEquals(AgreementPartySide.B, AgreementPartySide.of(a, 3L));
        assertEquals(AgreementPartySide.NONE, AgreementPartySide.of(a, 99L));
        assertEquals(AgreementPartySide.NONE, AgreementPartySide.of(a, null));
    }

    @Test
    @DisplayName("第三方会话看不到别人的协议，且回话是「不存在」（不泄露他人协议的存在性）")
    void thirdPartyCannotSeeAgreement() {
        Agreement a = agreement(2L, 3L);
        assertTrue(AgreementVisibility.canSee(a, 2L));
        assertTrue(AgreementVisibility.canSee(a, 3L));
        assertFalse(AgreementVisibility.canSee(a, 99L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementVisibility.assertVisible(a, 99L));
        assertEquals(404, e.getCode(), "第三方必须收到 404 而不是 403（403 会泄露存在性）");
    }

    // ══════════════════ 快照与哈希 ══════════════════

    @Test
    @DisplayName("快照是确定性的（同输入同结果），因此签名哈希稳定、可核对")
    void snapshotIsDeterministic() {
        Agreement a = agreement(2L, 3L);
        List<AgreementTerm> terms = List.of(term("CANCEL_POLICY", "BOTH_AGREE", null));
        List<AgreementTermOption> options = List.of(option("CANCEL_POLICY", "BOTH_AGREE", "取消需双方同意", false));
        String s1 = AgreementSnapshot.build(a, 1, terms, options, null, null);
        String s2 = AgreementSnapshot.build(a, 1, terms, options, null, null);
        assertEquals(s1, s2);
        assertEquals(AgreementSnapshot.sha256(s1), AgreementSnapshot.sha256(s2));
        assertEquals(64, AgreementSnapshot.sha256(s1).length(), "SHA-256 十六进制应为 64 字符");

        // 条款变了 ⇒ 哈希必须变（否则"确认的内容被偷改"就发现不了）
        String s3 = AgreementSnapshot.build(a, 1, List.of(term("CANCEL_POLICY", "ONE_SIDE", null)), options, null, null);
        assertFalse(AgreementSnapshot.sha256(s3).equals(AgreementSnapshot.sha256(s1)));
    }

    @Test
    @DisplayName("快照可自证内容：条款名与语义冗余存下，字典日后被改也不影响历史举证")
    void snapshotIsSelfContained() {
        Agreement a = agreement(2L, 3L);
        AgreementTermOption opt = option("RETURN_FREIGHT", "TO_SUPPLIER", "退回运费由供货方承担", true);
        opt.setSemantics("退货时运费从本期结算中扣给供货方");
        String json = AgreementSnapshot.build(a, 2, List.of(term("RETURN_FREIGHT", "TO_SUPPLIER", "100")),
                List.of(opt), null, null);
        List<AgreementSnapshot.TermEntry> entries = AgreementSnapshot.readTerms(json);
        assertEquals(1, entries.size());
        assertEquals("退回运费由供货方承担", entries.get(0).getOptionLabel());
        assertEquals("退货时运费从本期结算中扣给供货方", entries.get(0).getSemantics());
        assertEquals("100", entries.get(0).getParamValue());
    }

    // ══════════════════ 主档状态枚举口径 ══════════════════

    @Test
    @DisplayName("主档状态码与枚举一一对应（DB 存数字、接口发枚举名）")
    void agreementStatusCodes() {
        assertEquals(AgreementStatus.DRAFT, AgreementStatus.of(0));
        assertEquals(AgreementStatus.ACTIVE, AgreementStatus.of(1));
        assertEquals(AgreementStatus.SUSPENDED, AgreementStatus.of(2));
        assertEquals(AgreementStatus.TERMINATED, AgreementStatus.of(3));
        assertEquals("洽谈中", AgreementStatus.DRAFT.getLabel());
    }

    // ── 造数据的小工具 ──

    private static Agreement agreement(Long tenantA, Long tenantB) {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setAgreementNo("XY-20260922-0001");
        a.setAgreementType("DISTRIBUTION");
        a.setTitle("E2E 代销协议");
        a.setPartyAId(11L);
        a.setPartyATenantId(tenantA);
        a.setPartyBId(22L);
        a.setPartyBTenantId(tenantB);
        a.setStatus(AgreementStatus.DRAFT.getCode());
        return a;
    }

    private static AgreementVersion version(Integer status) {
        AgreementVersion v = new AgreementVersion();
        v.setId(200L);
        v.setAgreementId(1L);
        v.setVersionNo(2);
        v.setStatus(status);
        v.setSnapshotJson("{\"terms\":[]}");
        return v;
    }

    private static AgreementVersion fullySignedVersion() {
        AgreementVersion v = version(AgreementVersionStatus.DRAFT.getCode());
        v.setPartyAConfirmedBy(9L);
        v.setPartyAConfirmedAt(LocalDateTime.now());
        v.setPartyASignHash("hash-a");
        v.setPartyBConfirmedBy(8L);
        v.setPartyBConfirmedAt(LocalDateTime.now());
        v.setPartyBSignHash("hash-b");
        return v;
    }

    private static AgreementTerm term(String termCode, String optionCode, String param) {
        AgreementTerm t = new AgreementTerm();
        t.setAgreementId(1L);
        t.setVersionId(200L);
        t.setTermCode(termCode);
        t.setOptionCode(optionCode);
        t.setParamValue(param);
        return t;
    }

    private static AgreementTermOption option(String termCode, String optionCode, String label, boolean required) {
        AgreementTermOption o = new AgreementTermOption();
        o.setTermCode(termCode);
        o.setOptionCode(optionCode);
        o.setOptionLabel(label);
        o.setRequired(required);
        o.setStatus(1);
        o.setLegalReviewStatus("APPROVED");
        return o;
    }
}
