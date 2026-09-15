package cn.aiedge.dms.orderpool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单池台账行（池 + 任务主数据联查，页面不显示裸 taskId）
 */
@Data
@Schema(description = "订单池台账行")
public class OrderPoolRowVO {

    private Long id;

    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "关联订单号")
    private String orderNo;

    @Schema(description = "订单类型 1-销售配送 2-调拨 3-退货")
    private Integer orderType;

    private String orderTypeText;

    private String customerName;

    @Schema(description = "取货地址（发货地址）")
    private String sourceAddress;

    @Schema(description = "收货地址")
    private String customerAddress;

    @Schema(description = "预计里程(km)")
    private BigDecimal estimatedDistance;

    @Schema(description = "货品金额")
    private BigDecimal goodsAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "竞价模式 0-关闭 1-开启")
    private Integer bidEnabled;

    private BigDecimal bidStartPrice;

    @Schema(description = "当前竞价 / 中标价")
    private BigDecimal bidCurrentPrice;

    @Schema(description = "竞价数")
    private Integer bidCount;

    @Schema(description = "池状态 0-待抢单 1-竞价中 2-已接单 3-已过期 4-已下架")
    private Integer poolStatus;

    private String poolStatusText;

    private Long riderId;

    @Schema(description = "接单/中标配送员")
    private String riderName;

    private LocalDateTime publishedTime;

    private LocalDateTime bidStartTime;

    @Schema(description = "过期时间（竞价截止）")
    private LocalDateTime expireTime;

    @Schema(description = "剩余秒数（竞价中才有意义，<=0 表示已到时）")
    private Long remainSeconds;

    @Schema(description = "配送线路ID（任务侧线路档案引用，名称由前端按线路下拉映射）")
    private Long routeId;

    @Schema(description = "配送区域（线路档案区域快照）")
    private String routeArea;

    @Schema(description = "下架原因（人工下架留痕）")
    private String offlineReason;

    @Schema(description = "下架时间")
    private LocalDateTime offlineTime;

    private String remark;

    private LocalDateTime createTime;
}
