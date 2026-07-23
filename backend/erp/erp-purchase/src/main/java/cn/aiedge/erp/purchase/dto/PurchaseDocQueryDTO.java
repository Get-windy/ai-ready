package cn.aiedge.erp.purchase.dto;

import lombok.Data;

/**
 * 按单据Tab查询条件DTO（17个搜索字段）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDocQueryDTO {

    /** 日期范围-开始 */
    private String dateStart;

    /** 日期范围-结束 */
    private String dateEnd;

    /** 单据编号 */
    private String orderNo;

    /** 供应商名称 */
    private String supplierName;

    /** 经手人姓名 */
    private String purchaserName;

    /** 部门名称 */
    private String deptName;

    /** 制单人 */
    private String createByName;

    /** 仓库名称 */
    private String warehouseName;

    /** 单据状态 */
    private Integer status;

    /** 单据备注 */
    private String remark;

    /** 提交人 */
    private String submitterName;

    /** 审核人 */
    private String auditorName;

    /** 自定义字段1(数字)范围-开始 */
    private java.math.BigDecimal extNum1Start;

    /** 自定义字段1(数字)范围-结束 */
    private java.math.BigDecimal extNum1End;

    /** 自定义字段2(数字)范围-开始 */
    private java.math.BigDecimal extNum2Start;

    /** 自定义字段2(数字)范围-结束 */
    private java.math.BigDecimal extNum2End;

    /** 自定义字段3(文本) */
    private String extText1;

    /** 自定义字段4(文本) */
    private String extText2;

    /** 自定义字段5(文本) */
    private String extText3;

    /** 打印次数范围-开始 */
    private Integer printCountStart;

    /** 打印次数范围-结束 */
    private Integer printCountEnd;

    /** 分页-当前页 */
    private Long current = 1L;

    /** 分页-每页大小 */
    private Long size = 20L;
}
