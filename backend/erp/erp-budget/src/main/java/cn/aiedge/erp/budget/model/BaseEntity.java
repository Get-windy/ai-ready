package cn.aiedge.erp.budget.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 基础实体类
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /**
     * 逻辑删除标记
     */
    public void markAsDeleted() {
        this.deleted = true;
    }

    /**
     * 恢复删除
     */
    public void restore() {
        this.deleted = false;
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}
