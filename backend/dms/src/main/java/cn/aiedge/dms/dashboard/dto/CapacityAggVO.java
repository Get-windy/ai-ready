package cn.aiedge.dms.dashboard.dto;

import lombok.Data;

/**
 * 运力聚合中间结果（Mapper 出参，非接口返回）
 *
 * 同一类型被配送员聚合与车辆聚合复用：前者填 3 个配送员字段，后者填 2 个车辆字段。
 *
 * @author AI-Ready Team
 */
@Data
public class CapacityAggVO {

    private Long totalRiders;
    private Long activeRiders;
    private Long onlineRiders;

    private Long totalVehicles;
    private Long activeVehicles;
}
