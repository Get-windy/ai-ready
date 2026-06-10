package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.entity.SysRoleBillType;
import cn.aiedge.base.mapper.SysRoleBillTypeMapper;
import cn.aiedge.base.security.RbacService;
import cn.aiedge.base.security.SecurityUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色-单据类型权限服务
 * <p>
 * 借鉴 ql361 的 bill_type 级权限控制模型，实现单据类型级别的访问控制。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleBillTypeService extends ServiceImpl<SysRoleBillTypeMapper, SysRoleBillType> {

    private final SysRoleService roleService;
    private final RbacService rbacService;

    /**
     * 获取角色可访问的单据类型列表
     *
     * @param roleId 角色ID
     * @return 单据类型代码列表
     */
    public List<String> getRoleBillTypes(Long roleId) {
        return lambdaQuery()
                .eq(SysRoleBillType::getRoleId, roleId)
                .list()
                .stream()
                .map(SysRoleBillType::getBillType)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 获取用户可访问的所有单据类型（通过用户的所有角色聚合）
     *
     * @param userId 用户ID
     * @return 单据类型代码集合
     */
    public Set<String> getUserBillTypes(Long userId) {
        List<SysRole> roles = roleService.getUserRoles(userId);
        if (roles.isEmpty()) return Set.of();

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        return lambdaQuery()
                .in(SysRoleBillType::getRoleId, roleIds)
                .list()
                .stream()
                .map(SysRoleBillType::getBillType)
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户对指定单据类型的最大权限级别
     *
     * @param userId   用户ID
     * @param billType 单据类型代码
     * @return 最大权限级别（1=查看, 2=编辑, 3=审核），无可访问权限返回 0
     */
    public int getUserMaxPermissionLevel(Long userId, String billType) {
        List<SysRole> roles = roleService.getUserRoles(userId);
        if (roles.isEmpty()) return 0;

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        return lambdaQuery()
                .in(SysRoleBillType::getRoleId, roleIds)
                .eq(SysRoleBillType::getBillType, billType)
                .list()
                .stream()
                .mapToInt(SysRoleBillType::getPermissionLevel)
                .max()
                .orElse(0);
    }

    /**
     * 验证当前用户是否有权访问指定单据类型
     *
     * @param billType        单据类型代码
     * @param requiredLevel   所需最低权限级别（1=查看, 2=编辑, 3=审核）
     * @throws SecurityException 无权访问时抛出
     */
    public void validateBillTypeAccess(String billType, int requiredLevel) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("未登录，无法验证单据权限");
        }

        // 超级管理员跳过检查
        if (rbacService.isSuperAdmin(userId)) {
            return;
        }

        int maxLevel = getUserMaxPermissionLevel(userId, billType);
        if (maxLevel == 0) {
            log.warn("用户 {} 无权访问单据类型 {}", userId, billType);
            throw new SecurityException("无权访问单据类型: " + billType);
        }
        if (maxLevel < requiredLevel) {
            log.warn("用户 {} 对单据类型 {} 的权限不足（需要级别 {}, 实际 {}）",
                    userId, billType, requiredLevel, maxLevel);
            throw new SecurityException("权限不足，无法执行此操作");
        }
    }

    /**
     * 为角色分配单据类型权限（先清空再批量插入）
     *
     * @param roleId      角色ID
     * @param billTypes   单据类型权限列表（billType + permissionLevel）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignBillTypes(Long roleId, List<BillTypeAssignment> billTypes) {
        // 清空旧权限
        lambdaUpdate().eq(SysRoleBillType::getRoleId, roleId).remove();

        // 批量插入新权限
        if (billTypes == null || billTypes.isEmpty()) {
            log.info("已清空角色 {} 的所有单据类型权限", roleId);
            return;
        }

        List<SysRoleBillType> entities = billTypes.stream()
                .map(bt -> {
                    SysRoleBillType entity = new SysRoleBillType();
                    entity.setRoleId(roleId);
                    entity.setBillType(bt.billType());
                    entity.setPermissionLevel(bt.permissionLevel());
                    entity.setCreatedBy(SecurityUtils.getCurrentUserId());
                    return entity;
                })
                .collect(Collectors.toList());

        saveBatch(entities);
        log.info("已为角色 {} 分配 {} 个单据类型权限", roleId, entities.size());
    }

    /**
     * 获取角色单据类型权限详情（含 billType 中文名）
     */
    public List<BillTypeDetail> getRoleBillTypeDetails(Long roleId) {
        List<SysRoleBillType> records = lambdaQuery()
                .eq(SysRoleBillType::getRoleId, roleId)
                .list();
        if (records.isEmpty()) {
            // 返回所有 billType 的默认列表（level=0 表示无权限）
            return BILL_TYPE_NAMES.entrySet().stream()
                    .map(e -> new BillTypeDetail(e.getKey(), e.getValue(), 0))
                    .toList();
        }
        // 合并已分配的和未分配的
        java.util.Map<String, Integer> assigned = records.stream()
                .collect(java.util.stream.Collectors.toMap(
                        SysRoleBillType::getBillType,
                        SysRoleBillType::getPermissionLevel));
        return BILL_TYPE_NAMES.entrySet().stream()
                .map(e -> new BillTypeDetail(
                        e.getKey(),
                        e.getValue(),
                        assigned.getOrDefault(e.getKey(), 0)))
                .toList();
    }

    /**
     * 所有单据类型中文名映射
     */
    private static final java.util.Map<String, String> BILL_TYPE_NAMES = java.util.Map.of(
            "504", "采购订单",
            "601", "销售出库单",
            "604", "销售订单",
            "801", "收款单",
            "802", "付款单"
    );

    /**
     * 权限级别中文名
     */
    public static String getLevelName(int level) {
        return switch (level) {
            case 1 -> "查看";
            case 2 -> "编辑";
            case 3 -> "审核";
            default -> "无权限";
        };
    }

    /**
     * 单据类型权限分配DTO（含 billType + permissionLevel）
     */
    public record BillTypeAssignment(String billType, int permissionLevel) {}

    /**
     * 单据类型权限详情DTO（含 billType 中文名）
     */
    public record BillTypeDetail(String billType, String billTypeName, int permissionLevel) {}
}
