package cn.aiedge.erp.finance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有ERP实体类继承此类，提供通用 auditable 字段
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 创建者ID
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新者ID
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 删除标志 (0-正常, 1-已删除)
     */
    @Column(name = "deleted_flag", nullable = false, length = 1)
    private Integer deletedFlag = 0;
    
    /**
     * 租户ID (多租户支持)
     */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;
    
    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;
}
