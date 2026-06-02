package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 固定资产盘点实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_inventory", indexes = {
    @Index(name = "idx_fai_inventory_no", columnList = "inventory_no"),
    @Index(name = "idx_fai_asset_id", columnList = "asset_id"),
    @Index(name = "idx_fai_asset_code", columnList = "asset_code"),
    @Index(name = "idx_fai_status", columnList = "status")
})
public class FixedAssetInventory extends BaseEntity {

    @Column(name = "inventory_no", length = 50)
    private String inventoryNo;

    @Column(name = "inventory_date")
    private LocalDate inventoryDate;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "department_name", length = 100)
    private String departmentName;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_code", length = 50)
    private String assetCode;

    @Column(name = "asset_name", length = 200)
    private String assetName;

    @Column(name = "expected_location", length = 200)
    private String expectedLocation;

    @Column(name = "actual_location", length = 200)
    private String actualLocation;

    @Column(name = "expected_status", length = 20)
    private String expectedStatus;

    @Column(name = "actual_status", length = 20)
    private String actualStatus;

    @Column(name = "expected_custodian", length = 100)
    private String expectedCustodian;

    @Column(name = "actual_custodian", length = 100)
    private String actualCustodian;

    @Column(name = "check_result", length = 20)
    private String checkResult;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "status", length = 20)
    private String status = "pending";
}
