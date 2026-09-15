package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路-配送员绑定（区域分包）列表行
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "线路-配送员绑定")
public class RouteRiderVO {

    private Long id;

    private Long routeId;

    @Schema(description = "线路编号（快照）")
    private String routeCode;

    @Schema(description = "线路名称（快照）")
    private String routeName;

    private Long riderId;

    @Schema(description = "配送员姓名（快照）")
    private String riderName;

    @Schema(description = "配送员手机号（实时）")
    private String riderPhone;

    @Schema(description = "配送员状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer riderStatus;

    @Schema(description = "优先级（数值越小越优先）")
    private Integer priority;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
