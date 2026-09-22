package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementChangeOrder;
import cn.aiedge.agreement.domain.AgreementInviteGuard;
import cn.aiedge.agreement.domain.AgreementInviteToken;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementSignatureRules;
import cn.aiedge.agreement.domain.AgreementTerminationRules;
import cn.aiedge.agreement.domain.AgreementVersionDiff;
import cn.aiedge.agreement.dto.AgreementVersionDiffVO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementInvite;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSignature;
import cn.aiedge.agreement.entity.AgreementTermination;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.domain.AgreementPartySide;
import cn.aiedge.agreement.enums.AgreementInviteStatus;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementTerminationSource;
import cn.aiedge.agreement.enums.AgreementTerminationStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成立过程与终止的**纯规则**单测（不起 Spring、不连数据库）。
 *
 * <p>覆盖：唯一送达的五绑定逐条失配 · token 不可枚举与只存哈希的口径 ·
 * 签署必须带主体与授权依据 · 终止五种来源与状态推进 · 版本 diff。</p>
 */
class AgreementLifecycleRulesTest {

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;
    private static final Long PARTY_A = 11L;
    private static final Long PARTY_B = 22L;
    private static final Long USER = 9L;

    // ══════════════════════ 一、唯一送达：五绑定的每一种失配（§13.5） ══════════════════════

    @Test
    @DisplayName("未登录：打开邀请一律先要求登录，且文案是业务白话")
    void unloggedIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, null, null, PARTY_B, LocalDateTime.now()));
        assertEquals(401, e.getCode());
        assertTrue(e.getMessage().contains("请先登录"), e.getMessage());
    }

    @Test
    @DisplayName("会话租户不匹配（把链接转发给了别家）：明确拒绝，文案「这份契约不是发给你的」")
    void wrongTenantIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, 99L, PARTY_B, LocalDateTime.now()));
        assertEquals(403, e.getCode());
        assertTrue(e.getMessage().contains("这份契约不是发给你的"), e.getMessage());
    }

    @Test
    @DisplayName("所代表主体不匹配（同一家租户里的另一个人）：同样拒绝 —— 唯一送达认的是主体，不是租户")
    void wrongPartyIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, TENANT_B, 999L, LocalDateTime.now()));
        assertEquals(403, e.getCode());
        assertTrue(e.getMessage().contains("这份契约不是发给你的"), e.getMessage());
    }

    @Test
    @DisplayName("没声明代表哪个主体：先要求选择主体，而不是放行")
    void missingRepresentedPartyIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, TENANT_B, null, LocalDateTime.now()));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("代表哪个主体"), e.getMessage());
    }

    @Test
    @DisplayName("已过期：失效（并且与「已撤回」「已领取」是三种不同的原因）")
    void expiredIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(-1));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, TENANT_B, PARTY_B, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("已过期"), e.getMessage());
    }

    @Test
    @DisplayName("已撤回：失效，且不会被「也过期了」遮住 —— 三种失效分别可断言")
    void revokedIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.REVOKED, PARTY_B, TENANT_B, hours(-1));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, TENANT_B, PARTY_B, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("已被发起方撤回"), e.getMessage());
    }

    @Test
    @DisplayName("已使用（一次性）：失效，提示去看协议详情页")
    void acceptedIsRejected() {
        AgreementInvite invite = invite(AgreementInviteStatus.ACCEPTED, PARTY_B, TENANT_B, hours(24));
        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(invite, USER, TENANT_B, PARTY_B, LocalDateTime.now()));
        assertTrue(e.getMessage().contains("已经被领取过了"), e.getMessage());
    }

    @Test
    @DisplayName("五绑定全部匹配 ⇒ 放行；邀请不存在 ⇒ 404")
    void matchingInvitePasses() {
        AgreementInvite invite = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        AgreementInviteGuard.assertOpenable(invite, USER, TENANT_B, PARTY_B, LocalDateTime.now());
        assertTrue(AgreementInviteGuard.isOpenable(invite, LocalDateTime.now()));

        BusinessException e = assertThrows(BusinessException.class, () -> AgreementInviteGuard
                .assertOpenable(null, USER, TENANT_B, PARTY_B, LocalDateTime.now()));
        assertEquals(404, e.getCode());
    }

    @Test
    @DisplayName("撤回的前置：已被领取的不能撤回（要停止合作走终止流程）；已撤回的不用重复撤")
    void revokeGuards() {
        LocalDateTime now = LocalDateTime.now();
        AgreementInvite accepted = invite(AgreementInviteStatus.ACCEPTED, PARTY_B, TENANT_B, hours(24));
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementInviteGuard.assertRevocable(accepted, now)).getMessage().contains("已被对方领取"));

        AgreementInvite revoked = invite(AgreementInviteStatus.REVOKED, PARTY_B, TENANT_B, hours(24));
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementInviteGuard.assertRevocable(revoked, now)).getMessage().contains("无需重复撤回"));

        AgreementInvite pending = invite(AgreementInviteStatus.PENDING, PARTY_B, TENANT_B, hours(24));
        AgreementInviteGuard.assertRevocable(pending, now);
    }

    @Test
    @DisplayName("token：不可枚举（43 字符、两次不同）、只存哈希、hint 只给前 8 位、短链可供前端渲染二维码")
    void tokenIsUnguessableAndHashed() {
        String t1 = AgreementInviteToken.newToken();
        String t2 = AgreementInviteToken.newToken();
        assertEquals(43, t1.length(), "32 字节 Base64URL 无填充 = 43 字符");
        assertNotEquals(t1, t2, "两次生成必须不同（不可枚举）");

        String hash = AgreementInviteToken.hash(t1);
        assertEquals(64, hash.length(), "SHA-256 十六进制 = 64 字符");
        assertNotEquals(t1, hash, "库里存的是哈希，不是明文");
        assertTrue(AgreementInviteToken.matches(t1, hash));
        assertFalse(AgreementInviteToken.matches(t2, hash));
        assertFalse(AgreementInviteToken.matches(null, hash));

        assertEquals(t1.substring(0, 8), AgreementInviteToken.hint(t1));
        assertTrue(AgreementInviteToken.shortLink(t1).endsWith(t1));
        assertTrue(AgreementInviteToken.shortLink(t1).startsWith("/agreement/invite?token="));

        String code = AgreementInviteToken.newInviteCode();
        assertEquals(10, code.length());
        assertFalse(code.matches(".*[01OIL].*"), "人读邀请码里不应出现易混字符");
    }

    // ══════════════════════ 二、签署：必须带主体与授权依据（§13.6） ══════════════════════

    @Test
    @DisplayName("签署：授权依据必填 —— 没有授权依据的签署在举证时站不住脚")
    void authorityBasisIsRequired() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> AgreementSignatureRules.assertAuthority("   "));
        assertTrue(e.getMessage().contains("授权依据"), e.getMessage());
        AgreementSignatureRules.assertAuthority("法定代表人本人（营业执照登记的法定代表人）");
    }

    @Test
    @DisplayName("签署：所代表主体必须与协议这一端的缔约主体一致")
    void representedPartyMustMatchTheSide() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);

        AgreementSignatureRules.assertRepresentedParty(agreement, AgreementPartySide.A, PARTY_A);

        BusinessException e = assertThrows(BusinessException.class, () -> AgreementSignatureRules
                .assertRepresentedParty(agreement, AgreementPartySide.A, PARTY_B));
        assertTrue(e.getMessage().contains("不一致"), e.getMessage());

        assertThrows(BusinessException.class, () -> AgreementSignatureRules
                .assertRepresentedParty(agreement, AgreementPartySide.B, null));
    }

    @Test
    @DisplayName("签署：已生效版本不能再签；非缔约方不能签")
    void signableGuards() {
        AgreementVersion active = version(100L, 1, AgreementVersionStatus.ACTIVE.getCode(), "{\"terms\":[]}");
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementSignatureRules.assertSignable(active, AgreementPartySide.A))
                .getMessage().contains("不能再签署"));

        AgreementVersion draft = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementSignatureRules.assertSignable(draft, AgreementPartySide.NONE))
                .getMessage().contains("不是本协议的缔约方"));
        AgreementSignatureRules.assertSignable(draft, AgreementPartySide.B);
    }

    @Test
    @DisplayName("当前有效的签署 = 哈希与当前快照一致的最新一条（内容改了，旧签署自动失效）")
    void effectiveSignaturePicksTheOneMatchingCurrentContent() {
        AgreementSignature old = signature(1L, "旧哈希", LocalDateTime.now().minusDays(1));
        AgreementSignature fresh = signature(2L, "当前哈希", LocalDateTime.now());
        Optional<AgreementSignature> picked =
                AgreementSignatureRules.effectiveSignature(List.of(old, fresh), "当前哈希");
        assertTrue(picked.isPresent());
        assertEquals(2L, picked.get().getId());

        assertTrue(AgreementSignatureRules.effectiveSignature(List.of(old), "当前哈希").isEmpty(),
                "哈希对不上的签署不算有效签署");
        assertTrue(AgreementSignatureRules.effectiveSignature(List.of(), "当前哈希").isEmpty());
    }

    // ══════════════════════ 三、终止：五种来源与"终止≠免责"（§13.7） ══════════════════════

    @Test
    @DisplayName("五种来源：只有「协商一致」需要对方确认，其余四种发起即停止履行")
    void terminationSources() {
        assertEquals(5, AgreementTerminationSource.values().length, "五种来源，一个不多一个不少");
        assertTrue(AgreementTerminationSource.MUTUAL_AGREEMENT.isRequiresCounterpartyConfirm());
        for (AgreementTerminationSource s : AgreementTerminationSource.values()) {
            if (s == AgreementTerminationSource.MUTUAL_AGREEMENT) {
                continue;
            }
            assertFalse(s.isRequiresCounterpartyConfirm(), s + " 应当发起即生效");
            assertTrue(AgreementTerminationRules.takesEffectImmediately(s));
        }
        assertFalse(AgreementTerminationRules.takesEffectImmediately(AgreementTerminationSource.MUTUAL_AGREEMENT));
    }

    @Test
    @DisplayName("终止≠免责：给用户的口径必须是「只停止履行、不结清、不免责、平台不裁判」")
    void noExemptionNoticeIsExplicit() {
        String notice = AgreementTerminationRules.NO_EXEMPTION_NOTICE;
        assertTrue(notice.contains("停止履行"), notice);
        assertTrue(notice.contains("不会自动结清"), notice);
        assertTrue(notice.contains("不会自动免除"), notice);
        assertTrue(notice.contains("平台不裁判"), notice);
    }

    @Test
    @DisplayName("草稿协议不能终止、已终止不能重复终止；平台清退只能由平台侧发起")
    void requestableGuards() {
        Agreement draft = agreement(AgreementStatus.DRAFT.getCode(), null);
        assertTrue(assertThrows(BusinessException.class, () -> AgreementTerminationRules.assertRequestable(
                draft, AgreementTerminationSource.UNILATERAL, AgreementPartySide.A, TENANT_A))
                .getMessage().contains("还没有生效"));

        Agreement terminated = agreement(AgreementStatus.TERMINATED.getCode(), 100L);
        assertTrue(assertThrows(BusinessException.class, () -> AgreementTerminationRules.assertRequestable(
                terminated, AgreementTerminationSource.UNILATERAL, AgreementPartySide.A, TENANT_A))
                .getMessage().contains("已经是终止状态"));

        Agreement active = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        assertTrue(assertThrows(BusinessException.class, () -> AgreementTerminationRules.assertRequestable(
                active, AgreementTerminationSource.PLATFORM_EXPULSION, AgreementPartySide.A, TENANT_A))
                .getMessage().contains("平台清退只能由平台方发起"));

        // 自然到期要求协议本来约定了结束日期
        assertTrue(assertThrows(BusinessException.class, () -> AgreementTerminationRules.assertRequestable(
                active, AgreementTerminationSource.NATURAL_EXPIRY, AgreementPartySide.A, TENANT_A))
                .getMessage().contains("没有约定结束日期"));

        AgreementTerminationRules.assertRequestable(active, AgreementTerminationSource.UNILATERAL,
                AgreementPartySide.A, TENANT_A);
    }

    @Test
    @DisplayName("对方确认/异议只能对「待对方确认」的记录，且发起方不能替对方表态")
    void counterpartyActionGuards() {
        AgreementTermination pending = termination(AgreementTerminationStatus.PENDING, USER);

        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertCounterpartyActionable(pending, USER))
                .getMessage().contains("不能替对方确认"));

        AgreementTerminationRules.assertCounterpartyActionable(pending, USER + 1);

        AgreementTermination confirmed = termination(AgreementTerminationStatus.CONFIRMED, USER);
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertCounterpartyActionable(confirmed, USER + 1))
                .getMessage().contains("不需要再表态"));
    }

    @Test
    @DisplayName("异议：待确认时提 ⇒ 终止不成立；已终止后提 ⇒ 只留痕（不回滚终止事实）")
    void objectGuards() {
        AgreementTermination pending = termination(AgreementTerminationStatus.PENDING, USER);
        AgreementTermination confirmed = termination(AgreementTerminationStatus.CONFIRMED, USER);
        AgreementTerminationRules.assertObjectable(pending, USER + 1);
        AgreementTerminationRules.assertObjectable(confirmed, USER + 1);

        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertObjectable(pending, USER))
                .getMessage().contains("不能自己给自己提异议"));

        AgreementTermination withdrawn = termination(AgreementTerminationStatus.WITHDRAWN, USER);
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertObjectable(withdrawn, USER + 1))
                .getMessage().contains("不能提异议"));
    }

    @Test
    @DisplayName("撤回：只有发起人、且只有「待对方确认」的能撤")
    void withdrawGuards() {
        AgreementTermination pending = termination(AgreementTerminationStatus.PENDING, USER);
        AgreementTerminationRules.assertWithdrawable(pending, USER);
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertWithdrawable(pending, USER + 1))
                .getMessage().contains("只有终止的发起人"));

        AgreementTermination confirmed = termination(AgreementTerminationStatus.CONFIRMED, USER);
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementTerminationRules.assertWithdrawable(confirmed, USER))
                .getMessage().contains("只有「待对方确认」"));
    }

    @Test
    @DisplayName("变更单：终止过的协议不能改；基准版本必须属于本协议")
    void changeOrderGuards() {
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementChangeOrder.assertChangeable(true)).getMessage().contains("已终止的协议不能再发起变更"));
        AgreementChangeOrder.assertChangeable(false);

        AgreementVersion base = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        base.setAgreementId(1L);
        AgreementChangeOrder.assertBaseVersion(1L, base);
        assertTrue(assertThrows(BusinessException.class,
                () -> AgreementChangeOrder.assertBaseVersion(2L, base)).getMessage().contains("不属于本协议"));

        assertFalse(AgreementChangeOrder.NO_RETROACTIVE_NOTE.isBlank());
        assertTrue(AgreementChangeOrder.NO_RETROACTIVE_NOTE.contains("不追溯"));
    }

    // ══════════════════════ 四、版本 diff（§13.4 逐条可看） ══════════════════════

    @Test
    @DisplayName("diff：条款新增/删除/修改、设定新增/撤回、履约方式增删逐条给出，且保留顺序")
    void diffReportsEveryChange() {
        AgreementVersionDiff.Side before = new AgreementVersionDiff.Side(
                100L, 1, snapshot(term("RETURN_FREIGHT", "TO_SUPPLIER", null), term("CANCEL_POLICY", "BOTH_AGREE", null)),
                List.of(setting("AR_CREDIT_DAYS", new BigDecimal("30")), setting("COMMISSION_RATE", new BigDecimal("5"))),
                List.of(narrative("DISPUTE", "旧文字")),
                List.of(mode("DROP_SHIP")),
                Map.of("AR_CREDIT_DAYS", "账期天数", "COMMISSION_RATE", "佣金率"));

        AgreementVersionDiff.Side after = new AgreementVersionDiff.Side(
                200L, 2, snapshot(term("RETURN_FREIGHT", "TO_SELLER", null), term("QUALITY_LIABILITY", "SUPPLIER", null)),
                List.of(setting("AR_CREDIT_DAYS", new BigDecimal("45"))),
                List.of(narrative("DISPUTE", "新文字")),
                List.of(mode("DROP_SHIP"), mode("TRANSIT_STOCK")),
                Map.of("AR_CREDIT_DAYS", "账期天数", "COMMISSION_RATE", "佣金率"));

        AgreementVersionDiffVO vo = AgreementVersionDiff.compare(1L, "上一版（第 1 版）", before, after);

        assertEquals(200L, vo.getVersionId());
        assertEquals(100L, vo.getAgainstVersionId());
        assertEquals(Boolean.TRUE, vo.getSnapshotChanged());

        assertEquals(3, vo.getTerms().size(), "条款：改 1（退货路径）+ 删 1（取消承担）+ 增 1（质量责任）");
        assertEquals("CHANGED", changeType(vo.getTerms(), "RETURN_FREIGHT"));
        assertEquals("REMOVED", changeType(vo.getTerms(), "CANCEL_POLICY"));
        assertEquals("ADDED", changeType(vo.getTerms(), "QUALITY_LIABILITY"));

        assertEquals(2, vo.getSettings().size(), "设定：账期改了 + 佣金率被撤回（未约定）");
        assertEquals("CHANGED", changeType(vo.getSettings(), "AR_CREDIT_DAYS"));
        assertEquals("REMOVED", changeType(vo.getSettings(), "COMMISSION_RATE"));
        assertEquals("未约定", vo.getSettings().stream()
                .filter(i -> "COMMISSION_RATE".equals(i.getCode())).findFirst().orElseThrow().getAfterText());
        assertEquals("账期天数", vo.getSettings().stream()
                .filter(i -> "AR_CREDIT_DAYS".equals(i.getCode())).findFirst().orElseThrow().getLabel());

        assertEquals(1, vo.getNarratives().size(), "文字条款改过要报出来（但不解释法律含义）");
        assertEquals("CHANGED", vo.getNarratives().get(0).getChangeType());

        assertEquals(1, vo.getFulfillmentModes().size(), "履约方式：新增中转（可多种并存）");
        assertEquals("ADDED", vo.getFulfillmentModes().get(0).getChangeType());

        assertFalse(vo.getSummary().isEmpty());
        assertTrue(vo.getSummary().get(0).contains("条款变化"), vo.getSummary().toString());
    }

    @Test
    @DisplayName("diff：没有任何变化时如实说「都没有变化」；首版（无基准）视为全部新增")
    void diffHandlesNoChangeAndFirstVersion() {
        AgreementVersionDiff.Side same = new AgreementVersionDiff.Side(
                100L, 1, snapshot(term("CANCEL_POLICY", "BOTH_AGREE", null)),
                List.of(), List.of(), List.of(), Map.of());
        AgreementVersionDiff.Side same2 = new AgreementVersionDiff.Side(
                200L, 2, snapshot(term("CANCEL_POLICY", "BOTH_AGREE", null)),
                List.of(), List.of(), List.of(), Map.of());

        AgreementVersionDiffVO vo = AgreementVersionDiff.compare(1L, "上一版（第 1 版）", same, same2);
        assertEquals(Boolean.FALSE, vo.getSnapshotChanged());
        assertTrue(vo.getTerms().isEmpty());
        assertTrue(vo.getSummary().get(0).contains("都没有变化"), vo.getSummary().toString());

        AgreementVersionDiffVO first = AgreementVersionDiff.compare(1L, "首版（没有更早的版本可比）", null, same2);
        assertEquals(1, first.getTerms().size());
        assertEquals("ADDED", first.getTerms().get(0).getChangeType());
    }

    // ══════════════════════ 造数据 ══════════════════════

    private static String changeType(List<AgreementVersionDiffVO.Item> items, String code) {
        return items.stream().filter(i -> code.equals(i.getCode())).findFirst()
                .orElseThrow(() -> new AssertionError("diff 里没有 " + code)).getChangeType();
    }

    private static LocalDateTime hours(int h) {
        return LocalDateTime.now().plusHours(h);
    }

    private static AgreementInvite invite(AgreementInviteStatus status, Long targetParty, Long targetTenant,
                                          LocalDateTime expiresAt) {
        AgreementInvite invite = new AgreementInvite();
        invite.setId(500L);
        invite.setTenantId(0L);
        invite.setAgreementId(1L);
        invite.setVersionId(200L);
        invite.setTargetPartyId(targetParty);
        invite.setTargetTenantId(targetTenant);
        invite.setTargetSide("B");
        invite.setTokenHash(AgreementInviteToken.hash("raw-token"));
        invite.setTokenHint("raw-toke");
        invite.setInviteCode("ABCD234567");
        invite.setChannel("LINK");
        invite.setStatus(status.getCode());
        invite.setExpiresAt(expiresAt);
        invite.setViewCount(0);
        return invite;
    }

    private static Agreement agreement(Integer status, Long currentVersionId) {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setTenantId(0L);
        a.setAgreementNo("XY-20260922-0001");
        a.setAgreementType("DISTRIBUTION");
        a.setTitle("E2E 代销协议");
        a.setPartyAId(PARTY_A);
        a.setPartyATenantId(TENANT_A);
        a.setPartyBId(PARTY_B);
        a.setPartyBTenantId(TENANT_B);
        a.setStatus(status);
        a.setCurrentVersionId(currentVersionId);
        return a;
    }

    private static AgreementVersion version(Long id, int no, Integer status, String snapshot) {
        AgreementVersion v = new AgreementVersion();
        v.setId(id);
        v.setAgreementId(1L);
        v.setVersionNo(no);
        v.setStatus(status);
        v.setSnapshotJson(snapshot);
        return v;
    }

    private static AgreementSignature signature(Long id, String hash, LocalDateTime at) {
        AgreementSignature s = new AgreementSignature();
        s.setId(id);
        s.setSignHash(hash);
        s.setSignedAt(at);
        return s;
    }

    private static AgreementTermination termination(AgreementTerminationStatus status, Long requestedBy) {
        AgreementTermination t = new AgreementTermination();
        t.setId(600L);
        t.setAgreementId(1L);
        t.setSource(AgreementTerminationSource.MUTUAL_AGREEMENT.name());
        t.setStatus(status.getCode());
        t.setRequestedBy(requestedBy);
        t.setRequestedSide("A");
        t.setRequestedAt(LocalDateTime.now());
        t.setBasisText("第 2 版第 7 条");
        t.setClaimCounterpartyBreach(false);
        return t;
    }

    private static AgreementTerm term(String termCode, String optionCode, String param) {
        AgreementTerm t = new AgreementTerm();
        t.setTermCode(termCode);
        t.setOptionCode(optionCode);
        t.setParamValue(param);
        return t;
    }

    private static String snapshot(AgreementTerm... terms) {
        List<AgreementTermOption> options = new ArrayList<>();
        for (AgreementTerm t : terms) {
            AgreementTermOption o = new AgreementTermOption();
            o.setTermCode(t.getTermCode());
            o.setOptionCode(t.getOptionCode());
            o.setOptionLabel(t.getTermCode() + "选项");
            o.setStatus(1);
            options.add(o);
        }
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        return AgreementSnapshot.build(agreement, 1, List.of(terms), options, null, null);
    }

    private static AgreementSetting setting(String key, BigDecimal number) {
        AgreementSetting s = new AgreementSetting();
        s.setSettingKey(key);
        s.setValueType("NUMBER");
        s.setValueNumber(number);
        return s;
    }

    private static AgreementNarrative narrative(String sectionCode, String text) {
        AgreementNarrative n = new AgreementNarrative();
        n.setSectionCode(sectionCode);
        n.setSectionTitle(sectionCode);
        n.setContentText(text);
        n.setContentHash(AgreementSnapshot.sha256(text));
        return n;
    }

    private static AgreementFulfillmentMode mode(String mode) {
        AgreementFulfillmentMode m = new AgreementFulfillmentMode();
        m.setMode(mode);
        return m;
    }
}
