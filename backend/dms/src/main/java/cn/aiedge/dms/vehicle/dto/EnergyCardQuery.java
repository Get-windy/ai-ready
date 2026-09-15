package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 补能卡查询条件
 */
@Data
@Schema(description = "补能卡查询条件")
public class EnergyCardQuery {

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

    @Schema(description = "关键字（卡号 / 卡名称 / 发卡方）")
    private String keyword;

    @Schema(description = "卡类型：1-油卡 2-电卡 3-换电套餐 4-充电套餐 5-加气卡")
    private Integer cardType;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    @Schema(description = "绑定四轮车ID")
    private Long vehicleId;

    @Schema(description = "绑定骑手ID")
    private Long riderId;
}
