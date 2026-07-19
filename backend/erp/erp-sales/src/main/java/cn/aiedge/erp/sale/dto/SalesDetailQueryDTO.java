package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售明细查询参数DTO
 * 对应销售明细查询页的查询条件（~50个搜索条件）
 */
@Data
@Schema(description = "销售明细查询参数DTO")
public class SalesDetailQueryDTO {

    // ═══ 分页参数 ═══
    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    @Schema(description = "页码别名", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小别名", defaultValue = "20")
    private Integer pageSize = 20;

    // ═══ 日期范围 ═══
    @Schema(description = "日期类型（documentDate/createTime/bookkeepingTime/settlementTime）")
    private String dateType;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    // ═══ 单据基本信息 ═══
    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "单据类型（OUTBOUND/RETURN/EXCHANGE）")
    private String documentType;

    @Schema(description = "仓库名称")
    private String warehouseName;

    // ═══ 客户信息 ═══
    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户编号")
    private String customerCode;

    // ═══ 收货信息 ═══
    @Schema(description = "收货人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String receiverPhone;

    @Schema(description = "收货地址")
    private String shippingAddress;

    // ═══ 人员信息 ═══
    @Schema(description = "经手人名称")
    private String handlerName;

    @Schema(description = "默认经手人")
    private String defaultHandlerName;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "制单人")
    private String creatorName;

    @Schema(description = "记账人")
    private String bookkeeperName;

    // ═══ 商品信息（明细级） ═══
    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号")
    private String productCode;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "所属行业类别")
    private String industryCategory;

    @Schema(description = "商品行属性")
    private String productAttribute;

    // ═══ 价格查询 ═══
    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    // ═══ 布尔过滤 ═══
    @Schema(description = "是否赠品")
    private Boolean isGift;

    @Schema(description = "是否促销商品")
    private Boolean isPromoProduct;

    @Schema(description = "是否显示红冲")
    private Boolean showRed;

    @Schema(description = "是否仅统计车辆库")
    private Boolean onlyVehicleWarehouse;

    @Schema(description = "是否包含有来源订单的销售单据")
    private Boolean hasSourceOrder;

    // ═══ 物流信息 ═══
    @Schema(description = "配送方式")
    private String deliveryMethod;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "运单号")
    private String trackingNumber;

    @Schema(description = "配送司机")
    private String deliveryDriver;

    // ═══ 区域/备注 ═══
    @Schema(description = "区域")
    private String region;

    @Schema(description = "单据备注")
    private String remark;

    @Schema(description = "明细备注")
    private String itemRemark;

    @Schema(description = "买家备注")
    private String buyerRemark;

    @Schema(description = "客户备注")
    private String customerRemark;

    // ═══ 来源订单 ═══
    @Schema(description = "来源订单编号")
    private String sourceOrder;

    // ═══ 状态与类型 ═══
    @Schema(description = "结算状态")
    private String settlementStatus;

    @Schema(description = "产生方式")
    private String generationMethod;

    @Schema(description = "销售类型")
    private String salesType;

    // ═══ 结算时间 ═══
    @Schema(description = "结算完成时间起始")
    private String settlementTimeStart;

    @Schema(description = "结算完成时间结束")
    private String settlementTimeEnd;

    // ═══ 来源 ═══
    @Schema(description = "来源（PC/MOBILE/API/IMPORT）")
    private String source;

    // ═══ 表体自定义字段（数字范围 1-7） ═══
    @Schema(description = "表体自定义1最小值")
    private BigDecimal itemExtNum1Min;
    @Schema(description = "表体自定义1最大值")
    private BigDecimal itemExtNum1Max;

    @Schema(description = "表体自定义2最小值")
    private BigDecimal itemExtNum2Min;
    @Schema(description = "表体自定义2最大值")
    private BigDecimal itemExtNum2Max;

    @Schema(description = "表体自定义3最小值")
    private BigDecimal itemExtNum3Min;
    @Schema(description = "表体自定义3最大值")
    private BigDecimal itemExtNum3Max;

    @Schema(description = "表体自定义4最小值")
    private BigDecimal itemExtNum4Min;
    @Schema(description = "表体自定义4最大值")
    private BigDecimal itemExtNum4Max;

    @Schema(description = "表体自定义5最小值")
    private BigDecimal itemExtNum5Min;
    @Schema(description = "表体自定义5最大值")
    private BigDecimal itemExtNum5Max;

    @Schema(description = "表体自定义6最小值")
    private BigDecimal itemExtNum6Min;
    @Schema(description = "表体自定义6最大值")
    private BigDecimal itemExtNum6Max;

    @Schema(description = "表体自定义7最小值")
    private BigDecimal itemExtNum7Min;
    @Schema(description = "表体自定义7最大值")
    private BigDecimal itemExtNum7Max;

    // ═══ 表体自定义字段（文本 1-2） ═══
    @Schema(description = "表体自定义文本1")
    private String itemExtText1;

    @Schema(description = "表体自定义文本2")
    private String itemExtText2;

    // ═══ 表体自定义字段（往来单位/职员/部门） ═══
    @Schema(description = "表体自定义-往来单位ID")
    private Long itemExtPartner;

    @Schema(description = "表体自定义-职员ID")
    private Long itemExtStaff;

    @Schema(description = "表体自定义-部门ID")
    private Long itemExtDept;

    // ═══ 自定义getter保证分页参数兼容 ═══
    public Long getCurrent() {
        return current != null ? current : (pageNum != null ? (long) pageNum : 1L);
    }

    public Long getSize() {
        return size != null ? size : (pageSize != null ? (long) pageSize : 20L);
    }

    public Integer getPageNum() {
        if (pageNum == null && current != null) {
            pageNum = current.intValue();
        }
        return pageNum != null ? pageNum : 1;
    }

    public Integer getPageSize() {
        if (pageSize == null && size != null) {
            pageSize = size.intValue();
        }
        return pageSize != null ? pageSize : 20;
    }
}
