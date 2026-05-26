/**
 * ${Entity}实体类
 * 
 * 描述：${Description}
 * 作者：${Author}
 * 创建时间：${Date}
 * 最后修改时间：${Date}
 */
package cn.aiedge.erp.${module}.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ${Entity}实体
 */
@Entity
@Table(name = "${tableName}")
@Data
public class ${Entity} {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    /**
     * ${Field1} - ${field1Description}
     */
    @Column(name = "${field1Name}", nullable = false)
    private ${field1Type} ${field1Name};
    
    /**
     * ${Field2} - ${field2Description}
     */
    @Column(name = "${field2Name}")
    private ${field2Type} ${field2Name};
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 创建者
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 更新者
     */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    /**
     * 是否删除
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    @Column(name = "version")
    private Integer version;
    
    /**
     * 预插入操作
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    /**
     * 预更新操作
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}