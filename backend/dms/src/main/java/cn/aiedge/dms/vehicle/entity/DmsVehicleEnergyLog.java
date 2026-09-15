package cn.aiedge.dms.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补能流水（加油 / 充电 / 加气 / 换电）
 *
 * <p><b>归属模型「车/人二选一」</b>：四轮车挂 {@code vehicleId}（按仪表里程核算），
 * 骑手两轮车挂 {@code riderId}（换电多为月租套餐，按当班里程分摊）。二者至少填一个。</p>
 *
 * <p><b>为什么是流水而不是单据</b>：补能频次高（骑手一天 2~3 次换电）、无独立生命周期与单据号；
 * 有单据号/供应商/下次保养推算的（如《车辆维护》WB 维保号）才独立成单据。</p>
 *
 * <p>{@code mileageSinceLast} 与 {@code unitCost} 由服务端自动计算，前端不传。</p>
 */
@Data
@TableName("dms_vehicle_energy_log")
public class DmsVehicleEnergyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 四轮车ID（dms_vehicle.id；与 riderId 二选一） */
    private Long vehicleId;

    /** 骑手ID（dms_rider.id；两轮车换电/充电，与 vehicleId 二选一） */
    private Long riderId;

    /** 补能类型：1-汽油 2-柴油 3-充电 4-换电 5-加气 */
    private Integer energyType;

    /** 支付方式：1-现金 2-油卡 3-电卡 4-月租套餐 5-平台代扣 */
    private Integer payMode;

    /** 数量：L（油/气）/ kWh（充电）/ 次（换电） */
    private BigDecimal quantity;

    /** 单价（元 / L、元 / kWh、元 / 次） */
    private BigDecimal unitPrice;

    /** 金额（元） */
    private BigDecimal amountYuan;

    /** 本次仪表里程(km)（四轮车填写，用于算区间成本） */
    private Integer odometer;

    /** 区间里程(km)＝本次里程 − 上次同主体补能里程（服务端自动计算） */
    private Integer mileageSinceLast;

    /** 每公里成本（服务端自动计算＝金额 ÷ 区间里程） */
    private BigDecimal unitCost;

    /** 站点 / 商户 */
    private String station;

    /** 补能时间 */
    private LocalDateTime occurredAt;

    /** 卡号 / 月租套餐号 */
    private String cardNo;

    /** 凭证（发票 / 小票图片 URL） */
    private String voucherUrl;

    /** 异常标记：0-正常 1-异常 */
    private Integer abnormalFlag;

    /** 异常原因（里程倒挂 / 油耗超阈值 / 单位成本超阈值 / 金额非法） */
    private String abnormalReason;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
