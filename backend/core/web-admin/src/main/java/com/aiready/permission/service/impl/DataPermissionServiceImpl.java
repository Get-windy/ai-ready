package com.aiready.permission.service.impl;

import com.aiready.permission.entity.DataPermissionRule;
import com.aiready.permission.entity.Organization;
import com.aiready.permission.entity.Role;
import com.aiready.permission.mapper.DataPermissionRuleMapper;
import com.aiready.permission.mapper.OrganizationMapper;
import com.aiready.permission.service.DataPermissionService;
import com.aiready.permission.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限服务实现类
 */
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl extends ServiceImpl<DataPermissionRuleMapper, DataPermissionRule> 
        implements DataPermissionService {

    private final DataPermissionRuleMapper dataPermissionRuleMapper;
    private final OrganizationMapper organizationMapper;
    private final RoleService roleService;

    @Override
    public String getDataPermissionSql(String tableName, Long userId) {
        return getDataPermissionSql(tableName, "", userId);
    }

    @Override
    public String getDataPermissionSql(String tableName, String tableAlias, Long userId) {
        Integer dataScope = getDataScope(userId);
        
        // 全部数据权限，不需要过滤
        if (dataScope == 1) {
            return "";
        }
        
        String alias = tableAlias.isEmpty() ? "" : tableAlias + ".";
        DataPermissionRule rule = getRuleByTableName(tableName);
        
        String deptColumn = rule != null && rule.getDeptColumn() != null 
                ? rule.getDeptColumn() : "dept_id";
        String userColumn = rule != null && rule.getUserColumn() != null 
                ? rule.getUserColumn() : "create_by";
        
        StringBuilder sql = new StringBuilder();
        
        switch (dataScope) {
            case 2: // 本部门数据
                Long deptId = getUserDeptId(userId);
                if (deptId != null) {
                    sql.append(" AND ").append(alias).append(deptColumn).append(" = ").append(deptId);
                }
                break;
                
            case 3: // 本部门及子部门数据
                Set<Long> deptIds = getPermissionDeptIds(userId);
                if (!deptIds.isEmpty()) {
                    String deptIdStr = deptIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
                    sql.append(" AND ").append(alias).append(deptColumn).append(" IN (").append(deptIdStr).append(")");
                }
                break;
                
            case 4: // 仅本人数据
                sql.append(" AND ").append(alias).append(userColumn).append(" = ").append(userId);
                break;
                
            default:
                // 默认仅本人数据
                sql.append(" AND ").append(alias).append(userColumn).append(" = ").append(userId);
                break;
        }
        
        // 添加自定义SQL条件
        if (rule != null && rule.getCustomSql() != null && !rule.getCustomSql().isEmpty()) {
            sql.append(" AND ").append(rule.getCustomSql());
        }
        
        return sql.toString();
    }

    @Override
    public Set<Long> getPermissionDeptIds(Long userId) {
        Integer dataScope = getDataScope(userId);
        
        if (dataScope == 1) {
            // 全部数据，返回空集合表示不过滤
            return new HashSet<>();
        }
        
        Long userDeptId = getUserDeptId(userId);
        
        if (userDeptId == null) {
            return new HashSet<>();
        }
        
        if (dataScope == 2) {
            // 本部门数据
            Set<Long> deptIds = new HashSet<>();
            deptIds.add(userDeptId);
            return deptIds;
        }
        
        if (dataScope == 3) {
            // 本部门及子部门数据
            return getDeptAndChildrenIds(userDeptId);
        }
        
        // 仅本人数据，返回空集合
        return new HashSet<>();
    }

    @Override
    public Integer getDataScope(Long userId) {
        return roleService.getDataScopeByUserId(userId);
    }

    @Override
    public Long getUserDeptId(Long userId) {
        return organizationMapper.selectDeptIdByUserId(userId);
    }

    @Override
    public Set<Long> getDeptAndChildrenIds(Long deptId) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        
        // 递归获取所有子部门
        List<Organization> children = organizationMapper.selectChildrenByParentId(deptId);
        for (Organization child : children) {
            deptIds.add(child.getId());
            deptIds.addAll(getDeptAndChildrenIds(child.getId()));
        }
        
        return deptIds;
    }

    @Override
    public List<Organization> getOrganizationTree() {
        List<Organization> allOrgs = organizationMapper.selectList(
                new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getStatus, 1)
                        .orderByAsc(Organization::getSortOrder));
        
        return buildOrgTree(allOrgs, 0L);
    }

    /**
     * 构建组织树
     */
    private List<Organization> buildOrgTree(List<Organization> organizations, Long parentId) {
        List<Organization> tree = new ArrayList<>();
        
        for (Organization org : organizations) {
            if (parentId.equals(org.getParentId())) {
                List<Organization> children = buildOrgTree(organizations, org.getId());
                if (!children.isEmpty()) {
                    org.setChildren(children);
                }
                tree.add(org);
            }
        }
        
        return tree;
    }

    @Override
    public Organization getOrganizationByCode(String orgCode) {
        return organizationMapper.selectOne(
                new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getOrgCode, orgCode));
    }

    @Override
    public boolean hasDataPermission(Long userId, Long dataDeptId, Long dataUserId) {
        Integer dataScope = getDataScope(userId);
        
        // 全部数据权限
        if (dataScope == 1) {
            return true;
        }
        
        // 本部门数据
        if (dataScope == 2) {
            Long userDeptId = getUserDeptId(userId);
            return userDeptId != null && userDeptId.equals(dataDeptId);
        }
        
        // 本部门及子部门数据
        if (dataScope == 3) {
            Set<Long> deptIds = getPermissionDeptIds(userId);
            return deptIds.contains(dataDeptId);
        }
        
        // 仅本人数据
        if (dataScope == 4) {
            return userId.equals(dataUserId);
        }
        
        return false;
    }

    @Override
    public DataPermissionRule getRuleByTableName(String tableName) {
        return getOne(new LambdaQueryWrapper<DataPermissionRule>()
                .eq(DataPermissionRule::getTableName, tableName)
                .eq(DataPermissionRule::getStatus, 1));
    }
}
