package cn.aiedge.erp.purchase.entity;

import cn.aiedge.erp.purchase.enums.InquiryStatus;
import cn.aiedge.erp.purchase.enums.ApprovalStatus;
import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购询价单实体
 */
@Data
@Entity
@Table(name = "purchase_inquiry")
public class PurchaseInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_no", unique = true, nullable = false, length = 32)
    private String inquiryNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "inquiry_type", nullable = false, length = 20)
    private String inquiryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.DRAFT;

    @Column(name = "requirement_desc", columnDefinition = "TEXT")
    private String requirementDesc;

    @Column(name = "urgency_level", length = 10)
    private String urgencyLevel;

    @Column(name = "deadline_date", nullable = false)
    private LocalDateTime deadlineDate;

    @Column(name = "publish_date")
    private LocalDateTime publishDate;

    @Column(name = "close_date")
    private LocalDateTime closeDate;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "purchaser_id", nullable = false)
    private Long purchaserId;

    @Column(name = "invited_supplier_ids", columnDefinition = "TEXT")
    private String invitedSupplierIds;

    @Column(name = "quote_count")
    private Integer quoteCount = 0;
    
    @Column(name = "budget", precision = 18, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approval_comment", columnDefinition = "TEXT")
    private String approvalComment;

    @Column(name = "related_order_id")
    private Long relatedOrderId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Transient
    private List<PurchaseInquiryItem> items;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = InquiryStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}