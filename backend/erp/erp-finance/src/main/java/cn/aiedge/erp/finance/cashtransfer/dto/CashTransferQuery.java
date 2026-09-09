package cn.aiedge.erp.finance.cashtransfer.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 提存多条件分页查询条件
 * 按单据/按明细两套搜索字段。
 */
@Data
public class CashTransferQuery {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;

    /** 单据日期范围 */
    private LocalDate dateStart;

    private LocalDate dateEnd;

    /** 单据编号 */
    private String docNo;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String creatorName;

    /** 记账人 */
    private String bookkeeperName;

    /** 单据状态 */
    private Integer status;

    /** 转出账户 */
    private String fromAccountName;

    /** 转入账户（按明细分页专用） */
    private String toAccountName;

    /** 单据备注 */
    private String remark;

    /** 明细备注（按明细分页专用） */
    private String itemRemark;

    /** 显示红冲 */
    private Boolean showRed;
}
