package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统一销售单据查询参数DTO
 * 对应销售单据查询页的查询条件
 */
@Data
@Schema(description = "统一销售单据查询参数DTO")
public class UnifiedSalesDocQueryDTO {

    // ═══ 分页参数 ═══
    @Schema(description = "页码", defaultValue = "1")
    private Long current = 1L;

    @Schema(description = "每页大小", defaultValue = "20")
    private Long size = 20L;

    @Schema(description = "页码别名", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小别名", defaultValue = "20")
    private Integer pageSize = 20;

    // ═══ 日期与日期类型 ═══
    @Schema(description = "日期类型（documentDate/createTime/bookkeepingTime/sourceOrderDate）")
    private String dateType;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    // ═══ 单据基本信息 ═══
    @Schema(description = "单据编号")
    private String documentNo;

    @Schema(description = "单据类型（SALE_ORDER/OUTBOUND/RETURN/EXCHANGE）")
    private String documentType;

    // ═══ 客户信息 ═══
    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户编号")
    private String customerCode;

    @Schema(description = "客户级别")
    private String customerLevel;

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

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "制单人")
    private String creatorName;

    @Schema(description = "记账人")
    private String bookkeeperName;

    // ═══ 仓库 ═══
    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    // ═══ 状态与类型 ═══
    @Schema(description = "结算状态")
    private String settlementStatus;

    @Schema(description = "产生方式")
    private String generationMethod;

    @Schema(description = "销售类型")
    private String salesType;

    @Schema(description = "状态")
    private Integer status;

    // ═══ 来源订单 ═══
    @Schema(description = "来源订单编号")
    private String sourceOrder;

    @Schema(description = "来源订单日期起始")
    private String sourceOrderStartDate;

    @Schema(description = "来源订单日期结束")
    private String sourceOrderEndDate;

    // ═══ 备注 ═══
    @Schema(description = "单据备注")
    private String remark;

    @Schema(description = "买家备注")
    private String buyerRemark;

    @Schema(description = "商品行属性")
    private String productAttribute;

    // ═══ 自定义字段(数字范围) ═══
    @Schema(description = "自定义字段1最小值")
    private BigDecimal extNum1Min;

    @Schema(description = "自定义字段1最大值")
    private BigDecimal extNum1Max;

    @Schema(description = "自定义字段2最小值")
    private BigDecimal extNum2Min;

    @Schema(description = "自定义字段2最大值")
    private BigDecimal extNum2Max;

    // ═══ 自定义字段(文本) ═══
    @Schema(description = "自定义字段3(文本)")
    private String extText1;

    @Schema(description = "自定义字段4(文本)")
    private String extText2;

    @Schema(description = "自定义字段5(文本)")
    private String extText3;

    // ═══ 物流信息 ═══
    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "运单号")
    private String trackingNumber;

    // ═══ 区域 ═══
    @Schema(description = "区域")
    private String region;

    // ═══ 金额范围 ═══
    @Schema(description = "最小金额")
    private BigDecimal minAmount;

    @Schema(description = "最大金额")
    private BigDecimal maxAmount;

    @Schema(description = "最小本单金额")
    private BigDecimal minTotalAmount;

    @Schema(description = "最大本单金额")
    private BigDecimal maxTotalAmount;

    // ═══ 来源 ═══
    @Schema(description = "来源（PC/MOBILE/API/IMPORT）")
    private String source;

    // ═══ 特殊筛选 ═══
    @Schema(description = "是否显示红冲")
    private Boolean showRed;

    @Schema(description = "是否仅统计车辆库")
    private Boolean onlyVehicleWarehouse;

    @Schema(description = "应收来源过滤：true 时仅查询销售出库/退货/换货单，排除销售订单")
    private Boolean receivableOnly;

    // ═══ 各单据类型子类型过滤 ═══
    @Schema(description = "订单类型列表")
    private List<String> orderTypes;

    @Schema(description = "出库单类型列表")
    private List<String> outboundTypes;

    @Schema(description = "退货单类型列表")
    private List<String> returnTypes;

    @Schema(description = "换货单类型列表")
    private List<String> exchangeTypes;

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
