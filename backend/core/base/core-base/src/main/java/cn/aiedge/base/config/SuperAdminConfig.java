package cn.aiedge.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 超级管理员角色配置
 * 集中管理超管角色编码，避免硬编码
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "system.super-admin")
public class SuperAdminConfig {

    /**
     * 超级管理员角色编码集合
     * 支持配置多个角色编码作为超管标识
     * 默认值: SUPER_ADMIN, admin, super_admin
     */
    private Set<String> roleCodes = Set.of("SUPER_ADMIN", "admin", "super_admin");

    /**
     * 判断指定角色编码是否为超级管理员
     *
     * @param roleCode 角色编码
     * @return 是否为超管
     */
    public boolean isSuperAdminRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    /**
     * 判断角色编码集合中是否包含超级管理员
     *
     * @param roleCodes 角色编码集合
     * @return 是否包含超管
     */
    public boolean hasSuperAdminRole(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(this.roleCodes::contains);
    }

    /**
     * 判断角色编码集合中是否包含超级管理员（List版本）
     *
     * @param roleCodes 角色编码列表
     * @return 是否包含超管
     */
    public boolean hasSuperAdminRole(java.util.List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return roleCodes.stream().anyMatch(this.roleCodes::contains);
    }
}
