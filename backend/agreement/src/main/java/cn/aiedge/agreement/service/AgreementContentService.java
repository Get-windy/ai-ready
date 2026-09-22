package cn.aiedge.agreement.service;

import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementContentSaveResultVO;
import cn.aiedge.agreement.dto.AgreementContentVO;
import cn.aiedge.agreement.dto.AgreementEffectiveSettingsVO;
import cn.aiedge.agreement.dto.AgreementSettingDefVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 「协议内容层」服务：**字段设定版 + 文字版 + 履约方式集合 + 字段元数据**。
 *
 * <h3>三条纪律（都是 DOMAIN-MODEL §十三 的硬要求）</h3>
 * <ol>
 *   <li><b>只有草稿能改</b>：保存内容前一律过 {@code AgreementInvariants.assertVersionMutable}
 *       （已生效版本不可变，㉛）；</li>
 *   <li><b>可见性走唯一构造处</b>：所有按版本/协议读取的入口先过
 *       {@code AgreementVisibility}（裁定⑥），本服务不自己拼租户条件；</li>
 *   <li><b>未约定就是未约定</b>：读取结果用三态表达（已约定 / 未约定 / 未定义），
 *       绝不回落成默认值或 0（㉜）。</li>
 * </ol>
 */
public interface AgreementContentService {

    /** 平台字段元数据（含**消费方**：这一项会影响什么）。 */
    List<AgreementSettingDefVO> settingDefs();

    /** 取某版本的内容（设定 + 文字条款 + 履约方式集合 + 元数据 + 模板来源留痕）。 */
    AgreementContentVO content(Long versionId, Long sessionTenantId);

    /**
     * 保存某版本的内容（**整份覆盖**）。
     *
     * <p>只允许写草稿版本；内容有变化时会清空双方对该版本的确认痕迹 ——
     * 双签针对的是**内容**，内容一变，"我确认过"即失效（否则会出现
     * "对方确认后我偷偷改了设定仍算确认齐全"）。</p>
     */
    AgreementContentSaveResultVO saveContent(Long versionId, AgreementContentSaveDTO dto,
                                            Long sessionTenantId, Long operatorId);

    /**
     * 按**业务时点**取该协议生效那一版的设定（{@code AgreementRuntime} 的只读视图）。
     *
     * <p>给业务/客服与下游链路调试用：可核对"这笔单当时按的是哪一版、约定了什么"。</p>
     */
    AgreementEffectiveSettingsVO effective(Long agreementId, LocalDateTime businessTime, Long sessionTenantId);

    /**
     * 把**模板来源**写进某个草稿版本的快照（"基于模板 X，第 N 版"）—— 司法可追溯（§13.9）。
     *
     * <p>⚠️ 只记来源，**不改变任何设定值**：模板内容是否成为约定，仍取决于双方在那一版上的确认。</p>
     */
    void markTemplateSource(Long versionId, AgreementSnapshot.TemplateSource source,
                           Long sessionTenantId, Long operatorId);
}
