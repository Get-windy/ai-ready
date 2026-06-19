package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysDataScope;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 数据权限范围服务
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysDataScopeService extends IService<SysDataScope> {

    /**
     * 根据角色ID列表查询数据权限规则
     */
    List<SysDataScope> getByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID列表和目标表名查询数据权限规则（任一匹配即可）
     */
    List<SysDataScope> getByRoleAndTable(List<Long> roleIds, String tableName);

    /**
     * 保存角色的数据权限规则（先删后增）
     */
    void saveRoleDataScopes(Long roleId, List<SysDataScope> scopes);
}
