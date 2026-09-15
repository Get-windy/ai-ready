package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派单候选配送员（《智能调度开发文档》§3.4：逐单展示「选中配送员 + 命中规则 + 理由」）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "派单候选配送员")
public class DispatchCandidateVO {

    private Long riderId;

    private String riderName;

    private String riderPhone;

    @Schema(description = "车牌号（自带车辆，选人列表与车辆匹配展示）")
    private String vehicleNo;

    @Schema(description = "配送员当前纬度（地图派单落点）")
    private BigDecimal currentLat;

    @Schema(description = "配送员当前经度（地图派单落点）")
    private BigDecimal currentLng;

    @Schema(description = "配送员状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer status;

    @Schema(description = "到取货点直线距离(米)")
    private BigDecimal distanceMeters;

    @Schema(description = "当前在途单数")
    private Integer activeTasks;

    @Schema(description = "累计完成单量（选人参考）")
    private Integer totalOrders;

    private BigDecimal ratingScore;

    @Schema(description = "是否在线（最近位置上报在 dms.tracking.online.minutes 内）")
    private Boolean online;

    @Schema(description = "最近位置上报时间（在线口径的原始依据）")
    private LocalDateTime lastReportTime;

    @Schema(description = "区域分包：是否绑定了任务所属线路（AREA 策略下有效）")
    private Boolean routeBound;

    @Schema(description = "是否满足全部约束")
    private Boolean eligible;

    @Schema(description = "不满足约束时的原因（无人可派/超限/离线…）")
    private String reason;

    @Schema(description = "综合得分（越大越优先，按策略权重计算）")
    private BigDecimal score;
}
