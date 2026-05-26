package cn.aiedge.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
 * 用户实体类
 * 
 * 对应数据库表：users
 * 功能：用户基本信息管理
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    @Column(name = "full_name", length = 100)
    private String fullName;
    
    @Size(max = 20, message = "电话长度不能超过20个字符")
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "position", length = 100)
    private String position;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
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
     * 用户与角色的多对多关系
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
        indexes = {
            @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
            @Index(name = "idx_user_roles_role_id", columnList = "role_id")
        }
    )
    private Set<Role> roles = new HashSet<>();
    
    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE("活跃"),
        INACTIVE("停用"),
        LOCKED("锁定"),
        PENDING("待激活"),
        DELETED("已删除");
        
        private final String description;
        
        UserStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 添加角色
     */
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }
    
    /**
     * 移除角色
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }
    
    /**
     * 检查用户是否锁定
     */
    public boolean isLocked() {
        return status == UserStatus.LOCKED || 
               (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()));
    }
    
    /**
     * 检查用户是否活跃
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE && enabled && !isLocked();
    }
}