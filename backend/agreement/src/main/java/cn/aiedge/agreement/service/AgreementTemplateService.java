package cn.aiedge.agreement.service;

import cn.aiedge.agreement.dto.AgreementFromTemplateDTO;
import cn.aiedge.agreement.dto.AgreementTemplateDTO;
import cn.aiedge.agreement.dto.AgreementTemplateDetailVO;
import cn.aiedge.agreement.dto.AgreementTemplateQuery;
import cn.aiedge.agreement.dto.AgreementTemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 契约模板服务（§13.9）。
 *
 * <h3>两级 + 不对称的读写口径</h3>
 * <ul>
 *   <li><b>平台可读全部</b>（含租户模板）—— 合规抽查读权限，目的：避免非法交易；</li>
 *   <li><b>租户只读</b>「平台模板 + 自己的模板」；</li>
 *   <li><b>平台不能改租户模板</b>：能读不能改，否则就成了平台替租户定商业条款（㉜）。</li>
 * </ul>
 * 判定一律走 {@code AgreementTemplateVisibility}（唯一构造处）。</p>
 *
 * <h3>⚠️ 模板不是默认值</h3>
 * 模板项只在"从模板发起"时**预填**到新草稿版本，仍需双方在那一版上确认；
 * {@code AgreementRuntime} **绝不读模板**（"模板里有 ⇒ 视为已约定"是绝不允许的）。
 *
 * @param platformSide 当前会话是否平台侧（超管 / 系统租户）；由 Controller 判定后传入，
 *                     <b>不接受前端参数</b>（否则租户可以自称平台看别人的模板）
 */
public interface AgreementTemplateService {

    Page<AgreementTemplateVO> page(AgreementTemplateQuery query, Long sessionTenantId, boolean platformSide);

    AgreementTemplateDetailVO detail(Long id, Long sessionTenantId, boolean platformSide);

    Long create(AgreementTemplateDTO dto, Long sessionTenantId, boolean platformSide, Long operatorId);

    void update(Long id, AgreementTemplateDTO dto, Long sessionTenantId, boolean platformSide, Long operatorId);

    void delete(Long id, Long sessionTenantId, boolean platformSide);

    /**
     * 从模板发起契约：建主档 + 草稿版本 → 预填条款/设定/文字 → 把**模板来源**写进版本快照。
     *
     * @param platformSide 当前会话是否平台侧（决定"能读到哪些模板"）；不接受前端参数
     * @return 新建的协议 ID
     */
    Long apply(Long templateId, AgreementFromTemplateDTO dto, Long sessionTenantId, boolean platformSide,
               Long operatorId);
}
