package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 补能卡 / 套餐档案创建、修改请求
 */
@Data
@Schema(description = "补能卡创建请求")
public class EnergyCardCreateDTO {

    @NotBlank(message = "卡号/套餐号不能为空")
    @Schema(description = "卡号 / 套餐号（租户内唯一）")
    private String cardNo;

    @Schema(description = "卡名称 / 套餐名")
    private String cardName;

    @NotNull(message = "卡类型不能为空")
    @Schema(description = "卡类型：1-油卡 2-电卡 3-换电套餐 4-充电套餐 5-加气卡")
    private Integer cardType;

    @Schema(description = "绑定四轮车ID（与 riderId 二选一）")
    private Long vehicleId;

    @Schema(description = "绑定骑手ID（与 vehicleId 二选一）")
    private Long riderId;

    @Schema(description = "发卡方 / 运营商")
    private String issuer;

    @Schema(description = "月费（月租套餐）")
    private BigDecimal monthlyFee;

    @Schema(description = "余额（储值卡）")
    private BigDecimal balance;

    @Schema(description = "额度（套餐内可补能数量；为空=不限量）")
    private BigDecimal quota;

    @Schema(description = "生效日期")
    private LocalDate startDate;

    @Schema(description = "有效期至")
    private LocalDate expireDate;

    @Schema(description = "状态：0-停用 1-启用（缺省启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
