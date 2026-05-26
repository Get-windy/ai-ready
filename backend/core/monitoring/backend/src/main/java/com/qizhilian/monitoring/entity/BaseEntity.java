package com.qizhilian.monitoring.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有实体类的基类，包含通用字段
 * 
 * @author AI-Ready Team
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 创建人
     */
    @Column(name = "created_by", length = 64)
    private String createdBy;
    
    /**
     * 更新人
     */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;
    
    /**
     * 是否删除
     */
    @Column(name = "is_deleted", nullable = false)
    @JsonIgnore
    private Boolean isDeleted = false;
    
    /**
     * 版本号（用于乐观锁）
     */
    @Version
    @Column(name = "version")
    @JsonIgnore
    private Integer version = 0;
    
    /**
     * 租户ID（用于多租户隔离）
     */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;
    
    /**
     * 业务标识
     */
    @Column(name = "business_code", length = 128)
    private String businessCode;
    
    /**
     * 备注
     */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    /**
     * 额外信息（JSON格式）
     */
    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;
    
    /**
     * 数据源（用于数据同步跟踪）
     */
    @Column(name = "data_source", length = 32)
    private String dataSource;
    
    /**
     * 数据同步时间
     */
    @Column(name = "sync_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    
    /**
     * 数据同步状态
     */
    @Column(name = "sync_status", length = 32)
    private String syncStatus;
    
    /**
     * 数据标签
     */
    @Column(name = "tags", length = 512)
    private String tags;
    
    /**
     * 默认构造函数
     */
    public BaseEntity() {
        // 设置默认租户
        this.tenantId = "default";
        this.dataSource = "local";
        this.syncStatus = "synced";
    }
    
    /**
     * 带业务标识的构造函数
     * 
     * @param businessCode 业务标识
     */
    public BaseEntity(String businessCode) {
        this();
        this.businessCode = businessCode;
    }
    
    /**
     * 带租户ID的构造函数
     * 
     * @param tenantId 租户ID
     * @param businessCode 业务标识
     */
    public BaseEntity(String tenantId, String businessCode) {
        this();
        this.tenantId = tenantId;
        this.businessCode = businessCode;
    }
    
    /**
     * 逻辑删除
     */
    public void markAsDeleted() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 恢复删除
     */
    public void restoreFromDeleted() {
        this.isDeleted = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 设置创建信息
     * 
     * @param createdBy 创建人
     */
    public void setCreationInfo(String createdBy) {
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }
    
    /**
     * 设置更新信息
     * 
     * @param updatedBy 更新人
     */
    public void setUpdateInfo(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    /**
     * 添加标签
     * 
     * @param tag 标签
     */
    public void addTag(String tag) {
        if (this.tags == null || this.tags.isEmpty()) {
            this.tags = tag;
        } else {
            this.tags = this.tags + "," + tag;
        }
    }
    
    /**
     * 获取标签数组
     * 
     * @return 标签数组
     */
    public String[] getTagArray() {
        if (this.tags == null || this.tags.isEmpty()) {
            return new String[0];
        }
        return this.tags.split(",");
    }
    
    /**
     * 检查是否包含标签
     * 
     * @param tag 标签
     * @return 是否包含
     */
    public boolean containsTag(String tag) {
        if (this.tags == null || this.tags.isEmpty()) {
            return false;
        }
        return this.tags.contains(tag);
    }
    
    /**
     * 设置额外信息（JSON格式）
     * 
     * @param key 键
     * @param value 值
     */
    public void setExtraInfo(String key, String value) {
        // 简单的JSON构建，实际使用中可以使用JSON库
        if (this.extraInfo == null || this.extraInfo.isEmpty()) {
            this.extraInfo = String.format("{\"%s\":\"%s\"}", key, value);
        } else {
            // 这里简化处理，实际项目中应该使用JSON库
            this.extraInfo = this.extraInfo.replace("}", String.format(",\"%s\":\"%s\"}", key, value));
        }
    }
}