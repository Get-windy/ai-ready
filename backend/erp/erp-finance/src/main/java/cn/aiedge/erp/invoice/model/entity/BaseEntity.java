package cn.aiedge.erp.invoice.model.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有实体类的基类，包含公共字段
 */
@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 更新人ID
     */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    /**
     * 租户ID - 多租户支持
     */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;
    
    /**
     * 版本号 - 乐观锁
     */
    @Version
    @Column(name = "version")
    private Integer version = 0;
    
    /**
     * 是否删除 - 逻辑删除
     */
    @Column(name = "is_deleted")
    private Boolean deleted = false;
    
    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 删除人ID
     */
    @Column(name = "deleted_by", length = 50)
    private String deletedBy;
    
    /**
     * 扩展字段 - JSON格式存储
     */
    @Column(name = "extensions", columnDefinition = "TEXT")
    private String extensions;
    
    /**
     * 获取实体显示名称
     */
    public abstract String getDisplayName();
    
    /**
     * 获取实体类型
     */
    public abstract String getEntityType();
    
    /**
     * 逻辑删除
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    /**
     * 恢复删除
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
    
    /**
     * 检查是否已删除
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
    
    /**
     * 获取创建时间格式化字符串
     */
    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.toString() : "";
    }
    
    /**
     * 获取更新时间格式化字符串
     */
    public String getUpdatedAtFormatted() {
        return updatedAt != null ? updatedAt.toString() : "";
    }
    
    /**
     * 获取删除时间格式化字符串
     */
    public String getDeletedAtFormatted() {
        return deletedAt != null ? deletedAt.toString() : "";
    }
    
    /**
     * 获取审计信息
     */
    public String getAuditInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("创建: ").append(createdBy).append(" @ ").append(getCreatedAtFormatted());
        if (updatedBy != null) {
            sb.append(" | 更新: ").append(updatedBy).append(" @ ").append(getUpdatedAtFormatted());
        }
        if (isDeleted()) {
            sb.append(" | 删除: ").append(deletedBy).append(" @ ").append(getDeletedAtFormatted());
        }
        return sb.toString();
    }
    
    /**
     * 验证实体数据
     */
    public abstract boolean validate();
    
    /**
     * 准备持久化之前的操作
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (version == null) {
            version = 0;
        }
    }
    
    /**
     * 更新之前的操作
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 删除之前的操作
     */
    @PreRemove
    public void preRemove() {
        // 可以在这里添加删除前的逻辑
    }
    
    /**
     * 加载之后的操作
     */
    @PostLoad
    public void postLoad() {
        // 可以在这里添加加载后的逻辑
    }
}