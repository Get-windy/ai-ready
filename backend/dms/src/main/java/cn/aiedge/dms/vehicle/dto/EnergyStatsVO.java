package cn.aiedge.dms.vehicle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 能耗报表（P2）：汇总 + 油电对比 + 分组明细
 *
 * <p>核心指标是**每公里成本** —— 这是横向比较燃油车与电动车真实使用成本的公平口径；
 * CO₂ 折算用于绿色配送口径（汽油 2.30 kg/L、柴油 2.63 kg/L、电 0.581 kg/kWh）。</p>
 */
@Data
public class EnergyStatsVO {

    // ── 汇总 ──
    private Integer logCount;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    /** 有效里程合计（有区间里程的记录之和） */
    private Integer totalMileage;
    /** 平均每公里成本 ＝ 总金额 ÷ 有效里程 */
    private BigDecimal avgUnitCost;
    /** CO₂ 排放合计(kg) */
    private BigDecimal totalCo2Kg;

    // ── 油电对比 ──
    /** 燃油/燃气组（汽油/柴油/加气） */
    private EnergyGroup fuelGroup;
    /** 电动组（充电/换电） */
    private EnergyGroup powerGroup;

    // ── 分组明细 ──
    /** 按主体（车/人） */
    private List<EnergyGroup> bySubject;
    /** 按能源类型 */
    private List<EnergyGroup> byEnergyType;

    @Data
    public static class EnergyGroup {
        /** 分组键（主体ID / 能源类型值 / FUEL|POWER） */
        private String key;
        /** 分组显示名 */
        private String label;
        private Integer logCount;
        private BigDecimal amount;
        private BigDecimal quantity;
        private Integer mileage;
        /** 每公里成本 */
        private BigDecimal unitCost;
        private BigDecimal co2Kg;
    }
}
