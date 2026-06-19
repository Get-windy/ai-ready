package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysSodRule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 职责分离规则服务
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysSodRuleService extends IService<SysSodRule> {

    /**
     * 查询所有启用的 SoD 规则
     */
    List<SysSodRule> getActiveRules();

    /**
     * 验证角色分配是否违反 SoD 规则
     *
     * @param userId      目标用户ID
     * @param newRoleIds  要分配的角色ID列表
     * @return 冲突规则列表（空表示无冲突）
     */
    List<SysSodRule> validateRoleAssignment(Long userId, List<Long> newRoleIds);

    /**
     * 验证角色参数分配是否违反 SoD 规则
     *
     * @param roleIds 角色ID列表
     * @return 冲突角色ID列表
     */
    List<Long> findConflictingRoleIds(List<Long> roleIds);

    /**
     * 检查两个角色集合是否互斥
     */
    boolean hasConflict(List<Long> roleIdsA, List<Long> roleIdsB);
}
