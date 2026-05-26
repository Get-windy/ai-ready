package com.aiready.permission.service;

import com.aiready.permission.entity.DataPermissionRule;
import com.aiready.permission.entity.Organization;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 数据权限服务接口
 */
public interface DataPermissionService extends IService<DataPermissionRule> {
    
    /**
     * 获取用户的数据权限SQL条件
     */
    String getDataPermissionSql(String tableName, Long userId);
    
    /**
     * 获取用户的数据权限SQL条件（带表别名）
     */
    String getDataPermissionSql(String tableName, String tableAlias, Long userId);
    
    /**
     * 获取用户的数据权限部门ID集合
     */
    Set<Long> getPermissionDeptIds(Long userId);
    
    /**
     * 获取用户的数据权限范围
     * 1：全部数据 2：本部门数据 3：本部门及子部门数据 4：仅本人数据
     */
    Integer getDataScope(Long userId);
    
    /**
     * 获取用户的部门ID
     */
    Long getUserDeptId(Long userId);
    
    /**
     * 获取部门及其所有子部门ID
     */
    Set<Long> getDeptAndChildrenIds(Long deptId);
    
    /**
     * 获取组织树
     */
    List<Organization> getOrganizationTree();
    
    /**
     * 根据编码获取组织
     */
    Organization getOrganizationByCode(String orgCode);
    
    /**
     * 检查用户是否有数据权限
     */
    boolean hasDataPermission(Long userId, Long dataDeptId, Long dataUserId);
    
    /**
     * 获取数据权限规则
     */
    DataPermissionRule getRuleByTableName(String tableName);
}
