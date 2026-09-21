package cn.aiedge.erp.purchase.dto;

import lombok.Data;

/**
 * 采购合同分页查询条件。
 *
 * <p>字段与前端 `purchase-contract.ts` 的 {@code PurchaseContractQuery} 一一对应
 * （菜单 81010「采购合同」列表页的查询区）。</p>
 */
@Data
public class PurchaseContractQueryDTO {

    /** 当前页（从 1 开始） */
    private Long current = 1L;

    /** 每页条数 */
    private Long size = 10L;

    /** 合同编号（模糊） */
    private String contractNo;

    /** 合同标题（模糊） */
    private String contractTitle;

    /** 供应商名称（模糊） */
    private String supplierName;

    /** 供应商ID（精确） */
    private Long supplierId;

    /** 合同状态（DRAFT/PENDING_APPROVAL/APPROVED/ACTIVE/COMPLETED/REJECTED/TERMINATED/ARCHIVED） */
    private String contractStatus;

    /** 签订日期起（yyyy-MM-dd） */
    private String dateStart;

    /** 签订日期止（yyyy-MM-dd，含当日） */
    private String dateEnd;
}
