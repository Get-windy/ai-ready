package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 「物流/备注」批量更新请求
 *
 * <p>对齐 ql361 实测：《配发收 → 发货业务 → 物流发货》打开的「订单处理中心 → 2.拣货/发货」页，
 * 工具栏「物流备注」（批量，勾选多单）与行内「更多 → 物流/备注」（单条）共用同一个
 * `OrderRemarks` 弹窗（651×520），字段实测为：
 * 配送方式 / 司机 / 运单号 / 物流公司 / 收货人 / 联系电话 / 经手人 / 收货地址 / 销售类型 /
 * 自定义字段1(数字) / 2(数字) / 3(文本) / 4(文本) / 5(文本) / 单据备注。</p>
 *
 * <p><b>语义</b>：所有字段可空，<b>null 或空串 = 不改动该字段</b>（弹窗允许只填其中几项）。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "物流/备注批量更新（空值不修改）")
public class SaleLogisticsRemarkDTO {

    @Schema(description = "单据ID集合（销售订单ID）")
    private List<Long> ids;

    @Schema(description = "配送方式")
    private String deliveryMethod;

    @Schema(description = "司机ID")
    private Long driverId;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "运单号")
    private String waybillNo;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "物流公司档案ID（biz_party.id，partnerType=LOGISTICS）；传 id 时服务端自动补名称快照")
    private Long logisticsCompanyId;

    @Schema(description = "收货人")
    private String receiverName;

    @Schema(description = "联系电话")
    private String receiverPhone;

    @Schema(description = "经手人")
    private String salesmanName;

    @Schema(description = "收货地址")
    private String shippingAddress;

    @Schema(description = "销售类型")
    private Integer saleType;

    @Schema(description = "表头自定义字段1(数字) ←→ 自定义字段1(数字)")
    private BigDecimal extNum1;

    @Schema(description = "表头自定义字段2(数字) ←→ 自定义字段2(数字)")
    private BigDecimal extNum2;

    @Schema(description = "表头自定义字段1(文本) ←→ 自定义字段3(文本)")
    private String extText1;

    @Schema(description = "表头自定义字段2(文本) ←→ 自定义字段4(文本)")
    private String extText2;

    @Schema(description = "表头自定义字段3(文本) ←→ 自定义字段5(文本)")
    private String extText3;

    @Schema(description = "单据备注（弹窗「单据备注」字段）")
    private String orderRemark;

    /** 兼容旧入参名 `remark`（等价于 orderRemark） */
    @Schema(description = "单据备注（兼容旧字段名）")
    private String remark;
}
