package cn.aiedge.dms.vehicle.dto;

import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 补能流水行（流水 + 主体快照 + 字典文本）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnergyLogVO extends DmsVehicleEnergyLog {

    /** 主体类型：VEHICLE / RIDER */
    private String subjectType;

    /** 主体名称：车牌号 或 配送员姓名 */
    private String subjectName;

    /** 车牌号（四轮车） */
    private String plateNo;

    /** 配送员姓名（骑手两轮车） */
    private String riderName;

    /** 补能类型文本 */
    private String energyTypeText;

    /** 支付方式文本 */
    private String payModeText;

    /** 百公里油耗(L/100km)：派生值（＝数量×100 ÷ 区间里程），不落库 */
    private BigDecimal fuelPer100Km;
}
