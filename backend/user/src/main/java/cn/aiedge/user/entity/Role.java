package cn.aiedge.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 
 * 对应数据库表：roles
 * 功能：角色信息管理
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name", unique = true),
    @Index(name = "idx_role_code", columnList = "code", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度必须在2-50个字符之间")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
    
    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 50, message = "角色编码长度必须在2-50个字符之间")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    
    @Size(max = 200, message = "角色描述长度不能超过200个字符")
    @Column(name = "description", length = 200)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private RoleType type = RoleType.SYSTEM;
    
    @Column(name = "level")
    private Integer level = 1;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    /**
     * 角色与用户的多对多关系
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "roles")
    private Set<User> users = new HashSet<>();
    
    /**
     * 角色与权限的多对多关系
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"),
        indexes = {
            @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
            @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
        }
    )
    private Set<Permission> permissions = new HashSet<>();
    
    /**
     * 角色类型枚举
     */
    public enum RoleType {
        SYSTEM("系统角色"),
        BUSINESS("业务角色"),
        CUSTOM("自定义角色"),
        TEMPORARY("临时角色");
        
        private final String description;
        
        RoleType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 添加权限
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
        permission.getRoles().add(this);
    }
    
    /**
     * 移除权限
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
        permission.getRoles().remove(this);
    }
    
    /**
     * 检查角色是否有效
     */
    public boolean isEnabled() {
        return enabled && type != RoleType.TEMPORARY;
    }
}