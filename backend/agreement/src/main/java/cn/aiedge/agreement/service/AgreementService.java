package cn.aiedge.agreement.service;

import cn.aiedge.agreement.dto.AgreementCreateDTO;
import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.dto.AgreementSaveResultVO;
import cn.aiedge.agreement.dto.AgreementUpdateDTO;
import cn.aiedge.agreement.dto.AgreementVO;
import cn.aiedge.agreement.dto.AgreementVersionVO;
import cn.aiedge.agreement.dto.VersionCreateDTO;
import cn.aiedge.agreement.entity.AgreementTerm;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;

/**
 * 协议主档与版本的服务层。
 *
 * <p><b>会话租户必须显式传入</b>：协议四张表不参与租户拦截器（裁定⑥），
 * 所有可见性判断都依赖调用方把登录会话租户传进来。由 Controller 从
 * Sa-Token Session 取值传入，**不接受前端参数**。</p>
 */
public interface AgreementService {

    /** 分页查询（只返回本会话是其中一端的协议）。 */
    Page<AgreementVO> page(AgreementQuery query, Long sessionTenantId);

    /** 详情（含当前生效版本与条款）。 */
    AgreementVO detail(Long id, Long sessionTenantId);

    /** 新建协议草稿（自动生成协议编号 + 首个 DRAFT 版本），返回协议 ID。 */
    Long create(AgreementCreateDTO dto, Long sessionTenantId, Long operatorId);

    /** 修改草稿：主档字段 + 条款（条款写到指定 DRAFT 版本；整份覆盖）。 */
    AgreementSaveResultVO update(Long id, AgreementUpdateDTO dto, Long sessionTenantId, Long operatorId);

    /** 删除草稿（仅"洽谈中"可删）。 */
    void delete(Long id, Long sessionTenantId);

    /** 版本历史（含草稿与历史版本，各自带条款明细）。 */
    List<AgreementVersionVO> listVersions(Long id, Long sessionTenantId);

    /** 发起变更：从当前生效版本复制出新 DRAFT 版本，返回新版本 ID。 */
    Long createVersion(Long id, VersionCreateDTO dto, Long sessionTenantId, Long operatorId);

    /** 单个版本详情（含快照原文与条款明细）。 */
    AgreementVersionVO versionDetail(Long versionId, Long sessionTenantId);

    /**
     * 从**指定基准版本**复制出新 DRAFT 版本（**多轮协商的反要约**用，§13.4）。
     *
     * <p>与 {@link #createVersion} 的区别只在"以哪一版为基准"：协商里对方修改 =
     * 在**对方提的那一版**上改（否则上一轮的让步会被丢掉）；阶段修改则以现行生效版本为基准。
     * 两者共用同一套约束（同时只有一个活跃草稿）与同一条复制链路，因此不存在第二套实现。</p>
     *
     * <p>调用方（协商服务）需要先按"反要约 = 原要约失效"的语义把上一份提案置为 REJECTED，
     * 本方法才能通过"只有一个草稿"的检查。</p>
     */
    Long createVersionFrom(Long id, Long sourceVersionId, String changeReason,
                           Long sessionTenantId, Long operatorId);

    /** 本方确认签署（双签之一，本方由会话租户与两端 tenant 比对得出）。 */
    void confirm(Long versionId, Long sessionTenantId, Long operatorId);

    /**
     * 一方否决该 DRAFT 版本（置 REJECTED）。
     *
     * <p>⚠️ 关键语义：<b>现行 ACTIVE 版本继续有效、交易照常按它执行</b> ——
     * 本方法不触碰既有生效版本与主档状态（§3.4.4d2 变更流程里最容易做错的一步）。</p>
     */
    void reject(Long versionId, String reason, Long sessionTenantId, Long operatorId);

    /** 置为生效（校验必填条款齐 + 参数齐 + 双签齐 + 双方确认的就是当前快照）。 */
    void activate(Long versionId, Long sessionTenantId, Long operatorId);

    /**
     * 「这一版对某条款到底约定了什么」——<b>未约定就返回空，绝不回落到平台默认值</b>（§3.4.4d1）。
     *
     * <p>阶段 B/C 接单据与结算时，凡要拿"下单时刻生效的那一版"做判定的地方一律走本方法，
     * 并用 {@code isPresent()} 区分"约定了"与"未约定"。</p>
     */
    Optional<AgreementTerm> findAgreedTerm(Long versionId, String termCode, Long sessionTenantId);
}
