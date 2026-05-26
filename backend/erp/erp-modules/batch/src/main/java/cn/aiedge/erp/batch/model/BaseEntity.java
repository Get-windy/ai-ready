package cn.aiedge.erp.batch.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础实体类，包含审计字段
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    /**
     * 创建人
     */
    @CreatedBy
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 最后修改人
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    /**
     * 最后修改时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 是否删除（逻辑删除）
     */
    @Column(name = "deleted_flag", nullable = false)
    private Boolean deletedFlag = false;
    
    /**
     * 租户ID（多租户支持）
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
}