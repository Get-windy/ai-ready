package cn.aiedge.agreement;

import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementTermMapper;
import cn.aiedge.agreement.mapper.AgreementTermOptionMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.impl.AgreementServiceImpl;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 版本状态机在**服务层**的行为（用 Mockito 隔离数据库与 Spring）。
 *
 * <p>重点钉死两件最容易做错的事：</p>
 * <ol>
 *   <li><b>一方拒绝后，现行生效版本与主档状态一行都不许动</b>（变更谈成前交易照常 —— §3.4.4d2）；</li>
 *   <li><b>置生效时的顺序</b>：必须先把旧 ACTIVE 置 SUPERSEDED，再把新版本置 ACTIVE ——
 *       顺序颠倒会撞部分唯一索引 {@code uk_agreement_version_active}。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AgreementServiceStateMachineTest {

    /**
     * 纯单元测试没有 Spring/MyBatis 环境，MyBatis-Plus 的「实体 → 列名」映射表是空的，
     * 一旦服务层用到 {@code LambdaUpdateWrapper.set(Entity::getX, ...)} 就会抛
     * 「can not find lambda cache for this entity」。这里手工注册两张表的 TableInfo。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Agreement.class);
        TableInfoHelper.initTableInfo(assistant, AgreementVersion.class);
    }

    private static final Long TENANT_A = 2L;
    private static final Long TENANT_B = 3L;
    private static final Long USER = 9L;

    @Mock
    private AgreementMapper agreementMapper;
    @Mock
    private AgreementVersionMapper versionMapper;
    @Mock
    private AgreementTermMapper termMapper;
    @Mock
    private AgreementTermOptionMapper optionMapper;
    @Mock
    private BizNumberGeneratorService bizNumberGeneratorService;

    @InjectMocks
    private AgreementServiceImpl service;

    @Test
    @DisplayName("一方拒绝：该草稿置 REJECTED，而**现行生效版本与主档状态一行都不动**")
    void rejectOnlyMarksTheDraft() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion draft = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");

        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        service.reject(200L, "价格没谈拢", TENANT_A, USER);

        ArgumentCaptor<AgreementVersion> captor = ArgumentCaptor.forClass(AgreementVersion.class);
        verify(versionMapper, times(1)).updateById(captor.capture());
        assertEquals(200L, captor.getValue().getId(), "只应更新被否决的那个草稿版本");
        assertEquals(AgreementVersionStatus.REJECTED.getCode(), captor.getValue().getStatus());

        // 现行版本（100）与主档都不许被触碰 —— 这是"变更谈成前交易照常"的技术保证
        verify(agreementMapper, never()).updateById(any(Agreement.class));
        verify(versionMapper, never()).update(any(), any());
        assertEquals(100L, agreement.getCurrentVersionId(), "主档仍指向现行生效版本");
        assertEquals(AgreementStatus.ACTIVE.getCode(), agreement.getStatus(), "主档状态不变");
    }

    @Test
    @DisplayName("只有一方确认 ⇒ 置生效被拒，且不产生任何写操作")
    void activateRejectedWhenSingleSideSigned() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        draft.setPartyAConfirmedBy(USER);
        draft.setPartyAConfirmedAt(LocalDateTime.now());
        draft.setPartyASignHash(AgreementSnapshot.sha256(draft.getSnapshotJson()));

        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(optionMapper.selectList(any())).thenReturn(List.of());
        when(termMapper.selectList(any())).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.activate(200L, TENANT_A, USER));
        assertTrue(e.getMessage().contains("乙方"), e.getMessage());
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
        verify(agreementMapper, never()).updateById(any(Agreement.class));
    }

    @Test
    @DisplayName("必填条款没选完 ⇒ 置生效被拒，文案指出缺哪一项，且不产生任何写操作")
    void activateRejectedWhenRequiredTermMissing() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = fullySignedDraft("{\"terms\":[]}");
        AgreementTermOption required = option("RETURN_FREIGHT", "TO_SUPPLIER", true);

        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(optionMapper.selectList(any())).thenReturn(List.of(required));
        when(termMapper.selectList(any())).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.activate(200L, TENANT_A, USER));
        assertTrue(e.getMessage().contains("RETURN_FREIGHT"), e.getMessage());
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
        verify(agreementMapper, never()).updateById(any(Agreement.class));
    }

    @Test
    @DisplayName("双方确认的哈希与当前快照不一致（确认后条款被改）⇒ 置生效被拒")
    void activateRejectedWhenConfirmedContentChanged() {
        Agreement agreement = agreement(AgreementStatus.DRAFT.getCode(), null);
        AgreementVersion draft = version(200L, 1, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        draft.setPartyAConfirmedBy(USER);
        draft.setPartyAConfirmedAt(LocalDateTime.now());
        draft.setPartyASignHash("这不是当前快照的哈希");
        draft.setPartyBConfirmedBy(USER + 1);
        draft.setPartyBConfirmedAt(LocalDateTime.now());
        draft.setPartyBSignHash("这不是当前快照的哈希");

        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(optionMapper.selectList(any())).thenReturn(List.of());
        when(termMapper.selectList(any())).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.activate(200L, TENANT_A, USER));
        assertTrue(e.getMessage().contains("重新确认"), e.getMessage());
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
    }

    @Test
    @DisplayName("条件齐备 ⇒ 置生效：先旧版 SUPERSEDED、再新版 ACTIVE、最后主档指向新版本")
    void activateSupersedesOldVersionFirst() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion newVersion = fullySignedDraft("{\"terms\":[]}");

        when(versionMapper.selectById(200L)).thenReturn(newVersion);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(optionMapper.selectList(any())).thenReturn(List.of());
        when(termMapper.selectList(any())).thenReturn(List.of());
        // 不变量 6：没有别的生效协议
        when(agreementMapper.selectCount(any())).thenReturn(0L);

        service.activate(200L, TENANT_A, USER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<AgreementVersion>> supersedeCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        ArgumentCaptor<AgreementVersion> versionCaptor = ArgumentCaptor.forClass(AgreementVersion.class);
        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);

        // 顺序断言：必须先"把旧 ACTIVE 置 SUPERSEDED"再"把新版本置 ACTIVE"，
        // 否则会撞部分唯一索引 uk_agreement_version_active(agreement_id) WHERE status = 1
        InOrder inOrder = inOrder(versionMapper, agreementMapper);
        inOrder.verify(versionMapper).update(isNull(), supersedeCaptor.capture());
        inOrder.verify(versionMapper).updateById(versionCaptor.capture());
        inOrder.verify(agreementMapper).updateById(agreementCaptor.capture());

        assertTrue(supersedeCaptor.getValue().getSqlSet().contains("status"),
                "旧版本应被置为 SUPERSEDED");
        assertEquals(AgreementVersionStatus.ACTIVE.getCode(), versionCaptor.getValue().getStatus());
        assertEquals(200L, agreementCaptor.getValue().getCurrentVersionId(), "主档 current_version_id 应指向新版本");
        assertEquals(AgreementStatus.ACTIVE.getCode(), agreementCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("第三方会话对协议的任何写操作都被当作「协议不存在」拒掉（404，不泄露存在性）")
    void thirdPartyCannotOperate() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion draft = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), "{\"terms\":[]}");
        when(versionMapper.selectById(200L)).thenReturn(draft);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.confirm(200L, 99L, USER));
        assertEquals(404, e.getCode());
        verify(versionMapper, never()).updateById(any(AgreementVersion.class));
    }

    @Test
    @DisplayName("未约定的条款：服务层如实回答「未约定」（空），不回落到任何平台默认值")
    void undeclaredTermAnswersEmpty() {
        Agreement agreement = agreement(AgreementStatus.ACTIVE.getCode(), 100L);
        AgreementVersion active = version(100L, 1, AgreementVersionStatus.ACTIVE.getCode(), "{\"terms\":[]}");
        AgreementTerm cancelPolicy = new AgreementTerm();
        cancelPolicy.setTermCode("CANCEL_POLICY");
        cancelPolicy.setOptionCode("BOTH_AGREE");

        when(versionMapper.selectById(100L)).thenReturn(active);
        when(agreementMapper.selectById(1L)).thenReturn(agreement);
        when(termMapper.selectList(any())).thenReturn(List.of(cancelPolicy));

        assertTrue(service.findAgreedTerm(100L, "RETURN_FREIGHT", TENANT_A).isEmpty(),
                "未约定必须返回空 —— 平台不给默认值（§3.4.4d1）");
        assertTrue(service.findAgreedTerm(100L, "CANCEL_POLICY", TENANT_A).isPresent(),
                "已约定的条款必须如实返回");
    }

    // ── 造数据 ──

    private static Agreement agreement(Integer status, Long currentVersionId) {
        Agreement a = new Agreement();
        a.setId(1L);
        a.setTenantId(0L);
        a.setAgreementNo("XY-20260922-0001");
        a.setAgreementType("DISTRIBUTION");
        a.setTitle("E2E 代销协议");
        a.setPartyAId(11L);
        a.setPartyATenantId(TENANT_A);
        a.setPartyBId(22L);
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

    /** 双签齐 + 哈希与快照一致（= 可以进入"必填条款校验"的那一步）。 */
    private static AgreementVersion fullySignedDraft(String snapshot) {
        AgreementVersion v = version(200L, 2, AgreementVersionStatus.DRAFT.getCode(), snapshot);
        String hash = AgreementSnapshot.sha256(snapshot);
        v.setPartyAConfirmedBy(USER);
        v.setPartyAConfirmedAt(LocalDateTime.now());
        v.setPartyASignHash(hash);
        v.setPartyBConfirmedBy(USER + 1);
        v.setPartyBConfirmedAt(LocalDateTime.now());
        v.setPartyBSignHash(hash);
        return v;
    }

    private static AgreementTermOption option(String termCode, String optionCode, boolean required) {
        AgreementTermOption o = new AgreementTermOption();
        o.setTermCode(termCode);
        o.setOptionCode(optionCode);
        o.setOptionLabel(termCode);
        o.setRequired(required);
        o.setStatus(1);
        o.setLegalReviewStatus("APPROVED");
        return o;
    }
}
