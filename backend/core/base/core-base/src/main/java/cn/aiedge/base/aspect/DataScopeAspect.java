package cn.aiedge.base.aspect;

import cn.aiedge.base.annotation.DataScope;
import cn.aiedge.base.context.DataScopeContextHolder;
import cn.aiedge.base.entity.SysDataScope;
import cn.aiedge.base.entity.SysDept;
import cn.aiedge.base.entity.User;
import cn.aiedge.base.mapper.SysDeptMapper;
import cn.aiedge.base.mapper.SysUserRoleMapper;
import cn.aiedge.base.service.SysDataScopeService;
import cn.aiedge.base.service.UserService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限切面
 * 在方法执行前解析用户数据权限范围，生成SQL条件并存入ThreadLocal
 *
 * 数据权限规则：
 * - ALL: 无过滤条件
 * - DEPT: dept_id = currentDeptId
 * - DEPT_AND_CHILD: dept_id IN (childDeptIds)
 * - SELF: create_by = currentUserId
 * - CUSTOM_SQL: 执行自定义SQL条件（从 sys_data_scope 表获取）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final UserService userService;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDataScopeService sysDataScopeService;
    private final SysDeptMapper sysDeptMapper;
    private final ObjectMapper objectMapper;

    /** 角色数据权限类型常量 */
    private static final int DATA_SCOPE_ALL = 0;           // 全部数据
    private static final int DATA_SCOPE_DEPT = 1;          // 本部门数据
    private static final int DATA_SCOPE_DEPT_AND_CHILD = 2; // 本部门及子部门数据
    private static final int DATA_SCOPE_SELF = 3;          // 仅本人数据
    private static final int DATA_SCOPE_CUSTOM = 4;        // 自定义数据权限

    @Before("@annotation(dataScopeAnnotation)")
    public void doBefore(JoinPoint point, DataScope dataScopeAnnotation) {
        try {
            // 获取当前用户
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                // 未登录，不进行数据权限过滤
                DataScopeContextHolder.setDataScopeEnabled(false);
                return;
            }

            // 获取用户信息
            User user = userService.getById(userId);
            if (user == null) {
                DataScopeContextHolder.setDataScopeEnabled(false);
                return;
            }

            // 超级管理员跳过数据权限过滤
            if (user.getIsSuperAdmin() != null && user.getIsSuperAdmin()) {
                DataScopeContextHolder.setDataScopeEnabled(false);
                return;
            }

            // 设置目标表名
            String tableName = dataScopeAnnotation.tableName();
            if (tableName.isEmpty()) {
                // 从Mapper类名推断表名
                tableName = inferTableName(point);
            }
            DataScopeContextHolder.setTargetTable(tableName);

            // 优先使用用户个人数据权限
            String userScope = user.getDataScope();
            if (userScope != null && !userScope.isEmpty()) {
                String sqlCondition = buildSqlCondition(userScope, user, dataScopeAnnotation);
                if (sqlCondition != null && !sqlCondition.isEmpty()) {
                    DataScopeContextHolder.setDataScopeSql(sqlCondition);
                    DataScopeContextHolder.setDataScopeEnabled(true);
                } else {
                    DataScopeContextHolder.setDataScopeEnabled(false);
                }
                return;
            }

            // 获取用户角色ID列表
            List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
            if (roleIds == null || roleIds.isEmpty()) {
                // 无角色，只看本人数据
                String sqlCondition = buildSelfCondition(user, dataScopeAnnotation);
                DataScopeContextHolder.setDataScopeSql(sqlCondition);
                DataScopeContextHolder.setDataScopeEnabled(true);
                return;
            }

            // 从角色获取数据权限范围
            int roleDataScope = getRoleDataScope(roleIds);
            if (roleDataScope == DATA_SCOPE_ALL) {
                // 全部数据权限，不过滤
                DataScopeContextHolder.setDataScopeEnabled(false);
                return;
            }

            // 检查自定义数据权限规则
            if (roleDataScope == DATA_SCOPE_CUSTOM) {
                List<SysDataScope> customScopes = sysDataScopeService.getByRoleAndTable(roleIds, tableName);
                if (customScopes != null && !customScopes.isEmpty()) {
                    // 使用自定义规则
                    SysDataScope customScope = customScopes.get(0);
                    String sqlCondition = buildCustomSqlCondition(customScope, user, dataScopeAnnotation);
                    DataScopeContextHolder.setDataScopeSql(sqlCondition);
                    DataScopeContextHolder.setDataScopeEnabled(true);
                    return;
                }
            }

            // 根据角色数据权限类型生成SQL条件
            String scopeType = convertDataScopeType(roleDataScope);
            String sqlCondition = buildSqlCondition(scopeType, user, dataScopeAnnotation);

            if (sqlCondition != null && !sqlCondition.isEmpty()) {
                DataScopeContextHolder.setDataScopeSql(sqlCondition);
                DataScopeContextHolder.setDataScopeEnabled(true);
            } else {
                DataScopeContextHolder.setDataScopeEnabled(false);
            }

        } catch (Exception e) {
            log.error("数据权限解析失败", e);
            DataScopeContextHolder.setDataScopeEnabled(false);
        }
    }

    @After("@annotation(cn.aiedge.base.annotation.DataScope)")
    public void doAfter() {
        // 清除ThreadLocal
        DataScopeContextHolder.clear();
    }

    /**
     * 根据数据权限类型构建SQL条件
     */
    private String buildSqlCondition(String scopeType, User user, DataScope annotation) {
        switch (scopeType) {
            case "ALL":
                return null;
            case "DEPT":
                return buildDeptCondition(user, annotation);
            case "DEPT_AND_CHILD":
                return buildDeptAndChildCondition(user, annotation);
            case "SELF":
                return buildSelfCondition(user, annotation);
            default:
                return null;
        }
    }

    /**
     * 构建部门条件：dept_id = currentDeptId
     */
    private String buildDeptCondition(User user, DataScope annotation) {
        if (user.getDeptId() == null) {
            return null;
        }
        String deptAlias = annotation.deptAlias();
        String deptField = annotation.deptIdField();
        if (!deptAlias.isEmpty()) {
            return String.format("%s.%s = %d", deptAlias, deptField, user.getDeptId());
        }
        return String.format("%s = %d", deptField, user.getDeptId());
    }

    /**
     * 构建部门及子部门条件：dept_id IN (childDeptIds)
     */
    private String buildDeptAndChildCondition(User user, DataScope annotation) {
        if (user.getDeptId() == null) {
            return null;
        }

        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = user.getTenantId();
        }

        // 获取当前部门及所有子部门ID
        List<Long> deptIds = getAllChildDeptIds(user.getDeptId(), tenantId);
        deptIds.add(user.getDeptId());

        String deptAlias = annotation.deptAlias();
        String deptField = annotation.deptIdField();
        String deptIdList = deptIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        if (!deptAlias.isEmpty()) {
            return String.format("%s.%s IN (%s)", deptAlias, deptField, deptIdList);
        }
        return String.format("%s IN (%s)", deptField, deptIdList);
    }

    /**
     * 构建本人数据条件：create_by = currentUserId
     */
    private String buildSelfCondition(User user, DataScope annotation) {
        String userAlias = annotation.userAlias();
        String createByField = annotation.createByField();
        if (!userAlias.isEmpty()) {
            return String.format("%s.%s = %d", userAlias, createByField, user.getId());
        }
        return String.format("%s = %d", createByField, user.getId());
    }

    /**
     * 构建自定义SQL条件
     */
    private String buildCustomSqlCondition(SysDataScope scope, User user, DataScope annotation) {
        String ruleType = scope.getRuleType();
        if ("CUSTOM_SQL".equals(ruleType) && scope.getCustomSql() != null) {
            return scope.getCustomSql();
        }

        if ("DEPT".equals(ruleType) || "DEPT_AND_CHILD".equals(ruleType)) {
            // 使用 sys_data_scope.dept_ids 字段
            if (scope.getDeptIds() != null && !scope.getDeptIds().isEmpty()) {
                try {
                    List<Long> deptIds = objectMapper.readValue(scope.getDeptIds(), new TypeReference<List<Long>>() {});
                    if ("DEPT_AND_CHILD".equals(ruleType)) {
                        // 扩展子部门
                        Long tenantId = SecurityUtils.getCurrentTenantId();
                        if (tenantId == null) tenantId = user.getTenantId();
                        Set<Long> allDeptIds = new HashSet<>(deptIds);
                        for (Long deptId : deptIds) {
                            allDeptIds.addAll(getAllChildDeptIds(deptId, tenantId));
                        }
                        deptIds = new ArrayList<>(allDeptIds);
                    }
                    String deptAlias = annotation.deptAlias();
                    String deptField = annotation.deptIdField();
                    String deptIdList = deptIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                    if (!deptAlias.isEmpty()) {
                        return String.format("%s.%s IN (%s)", deptAlias, deptField, deptIdList);
                    }
                    return String.format("%s IN (%s)", deptField, deptIdList);
                } catch (Exception e) {
                    log.error("解析 deptIds JSON 失败: {}", scope.getDeptIds(), e);
                }
            }
        }

        if ("SELF".equals(ruleType)) {
            return buildSelfCondition(user, annotation);
        }

        return null;
    }

    /**
     * 获取角色的数据权限范围（取最大权限）
     */
    private int getRoleDataScope(List<Long> roleIds) {
        // 简单实现：取角色的最大数据权限范围
        // 实际项目中应该从 sys_role 表查询
        // 这里暂时返回自定义，后续需要完善
        return DATA_SCOPE_CUSTOM;
    }

    /**
     * 转换数据权限类型为字符串
     */
    private String convertDataScopeType(int dataScope) {
        switch (dataScope) {
            case DATA_SCOPE_ALL:
                return "ALL";
            case DATA_SCOPE_DEPT:
                return "DEPT";
            case DATA_SCOPE_DEPT_AND_CHILD:
                return "DEPT_AND_CHILD";
            case DATA_SCOPE_SELF:
                return "SELF";
            case DATA_SCOPE_CUSTOM:
                return "CUSTOM";
            default:
                return "SELF";
        }
    }

    /**
     * 获取所有子部门ID（递归）
     */
    private List<Long> getAllChildDeptIds(Long parentDeptId, Long tenantId) {
        List<Long> result = new ArrayList<>();
        collectChildDeptIds(parentDeptId, tenantId, result);
        return result;
    }

    /**
     * 递归收集子部门ID
     */
    private void collectChildDeptIds(Long parentDeptId, Long tenantId, List<Long> result) {
        List<SysDept> children = sysDeptMapper.selectChildrenByParentId(parentDeptId, tenantId);
        if (children != null && !children.isEmpty()) {
            for (SysDept child : children) {
                result.add(child.getId());
                collectChildDeptIds(child.getId(), tenantId, result);
            }
        }
    }

    /**
     * 从Mapper类推断表名
     */
    private String inferTableName(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?> declaringClass = signature.getDeclaringType();

        // 从类名推断表名
        // 如: SaleOrderMapper -> sale_order
        String className = declaringClass.getSimpleName();
        if (className.endsWith("Mapper")) {
            className = className.substring(0, className.length() - 6);
        }

        // 驼峰转下划线
        StringBuilder tableName = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                tableName.append('_');
            }
            tableName.append(Character.toLowerCase(c));
        }

        return tableName.toString();
    }
}