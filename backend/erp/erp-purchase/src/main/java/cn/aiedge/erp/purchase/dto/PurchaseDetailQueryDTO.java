package cn.aiedge.erp.purchase.dto;

import lombok.Data;



/**
 * 按明细Tab查询条件DTO（12个搜索字段）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDetailQueryDTO {

    /** 日期范围-开始 */
    private String dateStart;

    /** 日期范围-结束 */
    private String dateEnd;

    /** 单据编号 */
    private String orderNo;

    /** 来源订单(源单编号) */
    private String sourceBillNo;

    /** 商品名称 */
    private String productName;

    /** 供应商名称 */
    private String supplierName;

    /** 经手人 */
    private String purchaserName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String createByName;

    /** 审核人 */
    private String auditorName;

    /** 单据状态 */
    private Integer status;

    /** 仓库 */
    private String warehouseName;

    /** 单价状态 */
    private Integer priceStatus;

    /** 单据备注 */
    private String remark;

    /** 明细备注 */
    private String itemRemark;

    /** 是否赠品 */
    private Integer isGift;

    /** 分页-当前页 */
    private Long current = 1L;

    /** 分页-每页大小 */
    private Long size = 20L;
}
