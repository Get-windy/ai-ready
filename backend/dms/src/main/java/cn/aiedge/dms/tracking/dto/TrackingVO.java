package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轨迹点台账行（《配送跟踪开发文档》§3.2 列配置）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "轨迹点")
public class TrackingVO {

    private Long id;

    private Long riderId;

    @Schema(description = "配送员姓名（联查 dms_rider）")
    private String riderName;

    private String riderPhone;

    private Long taskId;

    @Schema(description = "任务编号（联查 dms_task）")
    private String taskNo;

    private String customerName;

    private BigDecimal lat;

    private BigDecimal lng;

    @Schema(description = "速度(km/h)")
    private BigDecimal speed;

    @Schema(description = "方向角度(0-360)")
    private BigDecimal direction;

    @Schema(description = "方向文案：北 / 东北 / 东 …")
    private String directionText;

    @Schema(description = "来源：1-APP 2-后台 3-渠道")
    private Integer source;

    private String sourceText;

    @Schema(description = "定位精度(米)")
    private BigDecimal accuracy;

    @Schema(description = "上报位置地址（上报端带回）")
    private String address;

    private LocalDateTime reportTime;

    @Schema(description = "段间里程(米)：与上一条同配送员轨迹点的直线距离（页内区间派生）")
    private BigDecimal segmentMeters;

    @Schema(description = "是否速度异常（>60km/h，供页面标红）")
    private Boolean speedAbnormal;
}
