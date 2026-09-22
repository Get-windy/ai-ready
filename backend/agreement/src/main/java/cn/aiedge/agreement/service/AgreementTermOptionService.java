package cn.aiedge.agreement.service;

import cn.aiedge.agreement.dto.TermOptionDTO;
import cn.aiedge.agreement.dto.TermOptionGroupVO;
import cn.aiedge.agreement.dto.TermOptionVO;

import java.util.List;

/**
 * 平台条款字典服务。
 *
 * <p>字典是**平台级参考数据**：写接口的权限码 {@code agreement:platform:term-option:manage}
 * 经最长前缀映射落在「系统」模块上（裁定⑤），而「系统」按 V11.455.0 只开给系统租户
 * ⇒ <b>平台协议的字典维护天然只有平台侧能用</b>，不必另造机制。</p>
 *
 * <p>⚠️ 本服务只维护"有哪些选项、各自什么含义"，**不提供任何默认值**（§3.4.4d1）。</p>
 */
public interface AgreementTermOptionService {

    /** 按条款类别分组下发（协议详情页一个下拉 = 一组）。 */
    List<TermOptionGroupVO> grouped();

    /** 某条款类别下的全部选项。 */
    List<TermOptionVO> listByTermCode(String termCode);

    /** 新增选项，返回新 ID。 */
    Long create(TermOptionDTO dto);

    /** 修改选项（历史协议快照不受影响）。 */
    void update(Long id, TermOptionDTO dto);

    /** 删除选项（软删；历史协议快照不受影响）。 */
    void delete(Long id);
}
