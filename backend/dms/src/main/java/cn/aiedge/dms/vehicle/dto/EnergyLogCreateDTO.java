package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补能流水创建 / 修改请求
 *
 * <p>{@code mileageSinceLast} 与 {@code unitCost} 为服务端派生字段，前端不传。</p>
 */
@Data
@Schema(description = "补能流水创建请求")
public class EnergyLogCreateDTO {

    @Schema(description = "四轮车ID（与 riderId 二选一）")
    private Long vehicleId;

    @Schema(description = "骑手ID（两轮车换电/充电，与 vehicleId 二选一）")
    private Long riderId;

    @NotNull(message = "补能类型不能为空")
    @Schema(description = "补能类型：1-汽油 2-柴油 3-充电 4-换电 5-加气")
    private Integer energyType;

    @Schema(description = "支付方式：1-现金 2-油卡 3-电卡 4-月租套餐 5-平台代扣")
    private Integer payMode;

    @Schema(description = "数量：L / kWh / 次")
    private BigDecimal quantity;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "金额（元）")
    private BigDecimal amountYuan;

    @Schema(description = "本次仪表里程(km)（四轮车填，用于自动算区间里程与每公里成本）")
    private Integer odometer;

    @Schema(description = "站点 / 商户")
    private String station;

    @Schema(description = "补能时间（缺省为当前时间）")
    private LocalDateTime occurredAt;

    @Schema(description = "卡号 / 月租套餐号")
    private String cardNo;

    @Schema(description = "凭证（发票/小票图片 URL）")
    private String voucherUrl;

    @Schema(description = "备注")
    private String remark;
}
