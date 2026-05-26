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
 * 权限实体类
 * 
 * 对应数据库表：permissions
 * 功能：权限信息管理
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name", unique = true),
    @Index(name = "idx_permission_code", columnList = "code", unique = true),
    @Index(name = "idx_permission_resource", columnList = "resource")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 100, message = "权限名称长度必须在2-100个字符之间")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    
    @NotBlank(message = "权限编码不能为空")
    @Size(min = 2, max = 100, message = "权限编码长度必须在2-100个字符之间")
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
    
    @Size(max = 200, message = "权限描述长度不能超过200个字符")
    @Column(name = "description", length = 200)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PermissionType type = PermissionType.MENU;
    
    @NotBlank(message = "资源路径不能为空")
    @Size(max = 200, message = "资源路径长度不能超过200个字符")
    @Column(name = "resource", nullable = false, length = 200)
    private String resource;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private PermissionAction action = PermissionAction.READ;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name = "level")
    private Integer level = 1;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "visible")
    private Boolean visible = true;
    
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
     * 权限与角色的多对多关系
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();
    
    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        MENU("菜单权限"),
        BUTTON("按钮权限"),
        API("API权限"),
        DATA("数据权限");
        
        private final String description;
        
        PermissionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 权限操作枚举
     */
    public enum PermissionAction {
        READ("查看"),
        CREATE("创建"),
        UPDATE("更新"),
        DELETE("删除"),
        EXECUTE("执行"),
        ADMIN("管理");
        
        private final String description;
        
        PermissionAction(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 检查权限是否有效
     */
    public boolean isEnabled() {
        return enabled;
    }
}