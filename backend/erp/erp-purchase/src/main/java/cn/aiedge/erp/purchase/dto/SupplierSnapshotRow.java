package cn.aiedge.erp.purchase.dto;

import lombok.Data;

/**
 * 供应商档案快照行（来自 erp_supplier 表，只读）
 * 用于补齐入库单缺失的供应商编号/联系人/电话/地址/备注。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SupplierSnapshotRow {

    private Long id;

    private String supplierCode;

    private String contactName;

    private String contactPhone;

    private String contactAddress;

    private String supplierRemark;
}
