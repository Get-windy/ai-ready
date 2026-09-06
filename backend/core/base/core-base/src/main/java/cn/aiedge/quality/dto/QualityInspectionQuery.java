package cn.aiedge.quality.dto;

import lombok.Data;

/**
 * 质检单分页查询条件
 */
@Data
public class QualityInspectionQuery {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页数量，默认 20 */
    private Integer pageSize = 20;

    /** 业务类型: PURCHASE_ORDER, SALE_ORDER, STOCK_IN, STOCK_OUT */
    private String bizType;

    /** 检验结果(等值): PASS, FAIL, PENDING, CONCESSION */
    private String result;

    /** 排除的检验结果(不等值): 已检验 Tab 传 PENDING */
    private String excludeResult;

    /** 来源单号(模糊) */
    private String bizNo;

    /** 质检单号(模糊) */
    private String qualityNo;

    /** 产品名称(模糊) */
    private String productName;

    /** 检验员(模糊) */
    private String inspectorName;

    /** 检验类型: INBOUND, OUTBOUND, PROCESS */
    private String inspectionType;

    /** 单据状态: 0待检, 1已完成, 2已作废 */
    private Integer status;

    /** 检验日期起(YYYY-MM-DD) */
    private String dateStart;

    /** 检验日期止(YYYY-MM-DD) */
    private String dateEnd;

    /** 全字段关键字(模糊，匹配单号/来源单号) */
    private String keyword;
}
