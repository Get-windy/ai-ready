package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时跟踪异常预警行（《实时跟踪开发文档》§4「异常预警：超速、偏航、异常停留、超时未送达」）
 *
 * <p>本轮落地三类可离线判定的预警：**超速**（speed > 阈值）、**异常停留**（相邻点位移 &lt;50 米且间隔超阈值）、
 * **超时在途**（任务已过要求完成时间仍未完成）。偏航需规划路径比对，依赖《路线规划》，列为遗留。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "跟踪异常预警")
public class TrackingAlertVO {

    @Schema(description = "预警类型：OVERSPEED-超速 STAY-异常停留 OVERDUE-超时在途")
    private String alertType;

    private String alertTypeText;

    /** 级别：WARN / DANGER */
    private String level;

    private Long riderId;

    private String riderName;

    private Long taskId;

    private String taskNo;

    private BigDecimal lat;

    private BigDecimal lng;

    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    @Schema(description = "指标值：速度(km/h) / 停留分钟 / 超时分钟")
    private BigDecimal value;

    private String alertText;
}
