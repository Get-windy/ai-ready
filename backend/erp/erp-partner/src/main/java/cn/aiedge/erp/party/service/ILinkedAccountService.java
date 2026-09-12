package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.dto.LinkedAccountQuery;
import cn.aiedge.erp.party.dto.LinkedAccountVO;
import cn.aiedge.erp.party.entity.LinkedAccount;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 互联账号服务（互联平台账号 ⇆ 往来单位 绑定关系）
 */
public interface ILinkedAccountService extends IService<LinkedAccount> {

    /** 分页查询（与导出同一过滤口径） */
    IPage<LinkedAccount> pageQuery(LinkedAccountQuery query, int pageNum, int pageSize);

    /** 实体 → VO 转换 */
    LinkedAccountVO toVO(LinkedAccount entity);

    /** 批量转换 */
    List<LinkedAccountVO> toVOList(List<LinkedAccount> entities);

    /** 状态切换（解绑 / 重新绑定）；status 仅接受 0/1 */
    boolean changeStatus(List<Long> ids, int status);

    /**
     * 查同平台同互联用户名的其它绑定（唯一约束前置校验；excludeId 为自身更新时排除）
     *
     * @return 冲突记录；无冲突返回 null
     */
    LinkedAccount findConflict(String platform, String linkedUserName, Long excludeId);

    /** 字典（全局唯一）：互联平台 + 关联类型 */
    Map<String, List<Map<String, String>>> dict();
}
