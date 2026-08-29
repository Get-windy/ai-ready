package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购费用分摊列表查询结果DTO
 *
 * 对应列表页13列：单据日期、单据编号、单据状态、记账时间、制单时间、本单金额、
 * 经手人、部门、制单人、记账人、摘要、单据备注、附件。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CostSharingPageDTO {

    private Long id;

    /** 单据日期 */
    private LocalDate sharingDate;

    /** 单据编号 */
    private String sharingNo;

    /** 单据状态 */
    private String status;

    /** 记账时间 */
    private LocalDateTime accountTime;

    /** 制单时间 */
    private LocalDateTime createTime;

    /** 本单金额 */
    private BigDecimal totalAmount;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String departmentName;

    /** 制单人 */
    private String createByName;

    /** 记账人 */
    private String bookkeeperName;

    /** 摘要 */
    private String summary;

    /** 单据备注 */
    private String remark;

    /** 附件 */
    private Integer attachment;

    /** 分摊方式 */
    private String allocationMethod;

    private String expenseType;

    /** 供应商名称 */
    private String supplierName;

    private LocalDateTime updateTime;
}
