package cn.aiedge.erp.supplier.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 供应商实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "supplier")
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", unique = true, nullable = false, length = 64)
    private String code;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "contact_person", length = 100)
    private String contactPerson;
    
    @Column(name = "contact_person_id")
    private Long contactPersonId;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "emergency_phone", length = 50)
    private String emergencyPhone;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "status")
    @Builder.Default
    private Integer status = 1;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
