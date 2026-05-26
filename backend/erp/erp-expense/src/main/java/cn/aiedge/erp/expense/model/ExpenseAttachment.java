package cn.aiedge.erp.expense.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 费用附件实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_attachment",
       indexes = {
           @Index(name = "idx_expense_attachment_application_id", columnList = "application_id"),
           @Index(name = "idx_expense_attachment_file_type", columnList = "file_type")
       })
public class ExpenseAttachment extends BaseEntity {

    @Column(name = "application_id", nullable = false, length = 50)
    private String applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private ExpenseApplication expenseApplication;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 200)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Column(name = "file_md5", length = 32)
    private String fileMd5;

    @Column(name = "attachment_type", length = 20)
    private String attachmentType;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "uploader_id", length = 50)
    private String uploaderId;

    @Column(name = "uploader_name", length = 100)
    private String uploaderName;

    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "remark", length = 500)
    private String remark;
}