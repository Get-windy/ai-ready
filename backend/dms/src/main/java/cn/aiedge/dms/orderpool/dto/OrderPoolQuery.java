package cn.aiedge.dms.orderpool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单池台账查询条件（《订单池开发文档》§3.1）
 */
@Data
@Schema(description = "订单池查询条件")
public class OrderPoolQuery {

    @Schema(description = "任务编号（模糊）")
    private String taskNo;

    @Schema(description = "关联订单号（模糊）")
    private String orderNo;

    @Schema(description = "客户（模糊）")
    private String customerName;

    @Schema(description = "订单类型 1-销售配送 2-调拨 3-退货")
    private Integer orderType;

    @Schema(description = "池状态 0-待抢单 1-竞价中 2-已接单 3-已过期 4-已下架")
    private Integer poolStatus;

    @Schema(description = "竞价模式 0-关闭 1-开启")
    private Integer bidEnabled;

    @Schema(description = "接单配送员ID")
    private Long riderId;

    @Schema(description = "配送线路ID（erp_route，任务上的线路档案引用）")
    private Long routeId;

    @Schema(description = "配送区域（线路档案区域快照，模糊）")
    private String routeArea;

    @Schema(description = "发布时间-起（yyyy-MM-dd）")
    private String publishTimeStart;

    @Schema(description = "发布时间-止（yyyy-MM-dd）")
    private String publishTimeEnd;

    @Schema(description = "配送费下限")
    private BigDecimal feeMin;

    @Schema(description = "配送费上限")
    private BigDecimal feeMax;

    @Schema(description = "关键字（任务编号/订单号/客户）")
    private String keyword;
}
