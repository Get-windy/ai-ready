package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 按单据Tab列表返回DTO（39列）
 * 对标文档定义的按单据列表字段
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseOrderListDTO {

    /** 1 单据日期 */
    private LocalDateTime orderDate;

    /** 2 单据编号 */
    private String orderNo;

    /** 3 源单 */
    private String sourceBillNo;

    /** 4 单据状态 */
    private Integer status;

    /** 5 仓库 */
    private String warehouseName;

    /** 6 供应商名称 */
    private String supplierName;

    /** 7 供应商编号 */
    private String supplierCode;

    /** 8 联系人 */
    private String contactName;

    /** 9 联系电话 */
    private String contactPhone;

    /** 10 联系地址 */
    private String contactAddress;

    /** 11 供应商备注 */
    private String supplierRemark;

    /** 12 经手人 */
    private String purchaserName;

    /** 13 部门 */
    private String deptName;

    /** 14 商品金额 */
    private BigDecimal productAmount;

    /** 15 直接优惠 */
    private BigDecimal discountAmount;

    /** 16 其他费用 */
    private BigDecimal otherExpense;

    /** 17 本单金额 */
    private BigDecimal billAmount;

    /** 18 已结金额 */
    private BigDecimal settledAmount;

    /** 19 预计收货时间 */
    private LocalDateTime expectedReceiveTime;

    /** 20 订货数量 */
    private BigDecimal totalQuantity;

    /** 21 已收数量 */
    private BigDecimal receivedQuantity;

    /** 22 未收数量 */
    private BigDecimal unreceiveQuantity;

    /** 23 退货数量 */
    private BigDecimal returnQuantity;

    /** 24 退货金额 */
    private BigDecimal returnAmount;

    /** 25 重量(kg) */
    private BigDecimal weight;

    /** 26 体积(m³) */
    private BigDecimal volume;

    /** 27 单据备注 */
    private String remark;

    /** 28 摘要 */
    private String summary;

    /** 29 附件 */
    private String attachment;

    /** 30 表头自定义字段1(数字) */
    private BigDecimal extNum1;

    /** 31 表头自定义字段2(数字) */
    private BigDecimal extNum2;

    /** 32 表头自定义字段3(文本) */
    private String extText1;

    /** 33 表头自定义字段4(文本) */
    private String extText2;

    /** 34 表头自定义字段5(文本) */
    private String extText3;

    /** 35 提交时间 */
    private LocalDateTime submitTime;

    /** 36 制单人 */
    private String createByName;

    /** 37 提交人 */
    private String submitterName;

    /** 38 审核人 */
    private String auditorName;

    /** 39 打印次数 */
    private Integer printCount;

    /** 主键ID（用于操作列） */
    private Long id;
}
