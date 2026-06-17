package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.CleanupRule;
import java.util.List;

/**
 * 数据清理规则服务接口
 */
public interface CleanupRuleService {

    List<CleanupRule> list(Long tenantId);

    CleanupRule create(CleanupRule rule, Long tenantId, String createBy);

    CleanupRule update(Long id, CleanupRule rule, Long tenantId, String updateBy);

    boolean delete(Long id);

    boolean execute(Long id);
}
