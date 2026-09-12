package cn.aiedge.erp.finance.arapadjust.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 应收应付调整多条件分页查询条件
 * 对应列表页查询方案（单表，不拆按单据/按明细）。
 */
@Data
public class ArApAdjustQuery {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;

    /** 单据日期范围 */
    private LocalDate dateStart;

    private LocalDate dateEnd;

    /** 单据编号 */
    private String docNo;

    /** 结算单位 */
    private String partnerName;

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

    /** 调账类型（即调整方向） */
    private Integer direction;

    /** 单据备注 */
    private String remark;

    /** 显示红冲 */
    private Boolean showRed;
}
