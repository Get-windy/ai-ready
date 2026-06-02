package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产转移实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_transfer", indexes = {
    @Index(name = "idx_fat_transfer_no", columnList = "transfer_no"),
    @Index(name = "idx_fat_asset_id", columnList = "asset_id"),
    @Index(name = "idx_fat_asset_code", columnList = "asset_code"),
    @Index(name = "idx_fat_status", columnList = "status")
})
public class FixedAssetTransfer extends BaseEntity {

    @Column(name = "transfer_no", length = 50)
    private String transferNo;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_code", length = 50)
    private String assetCode;

    @Column(name = "asset_name", length = 200)
    private String assetName;

    @Column(name = "from_department_id", length = 50)
    private String fromDepartmentId;

    @Column(name = "from_department_name", length = 100)
    private String fromDepartmentName;

    @Column(name = "to_department_id", length = 50)
    private String toDepartmentId;

    @Column(name = "to_department_name", length = 100)
    private String toDepartmentName;

    @Column(name = "from_custodian_id", length = 50)
    private String fromCustodianId;

    @Column(name = "from_custodian_name", length = 100)
    private String fromCustodianName;

    @Column(name = "to_custodian_id", length = 50)
    private String toCustodianId;

    @Column(name = "to_custodian_name", length = 100)
    private String toCustodianName;

    @Column(name = "transfer_date")
    private LocalDate transferDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "approval_comment", length = 500)
    private String approvalComment;

    @Column(name = "transfer_time")
    private LocalDateTime transferTime;
}
