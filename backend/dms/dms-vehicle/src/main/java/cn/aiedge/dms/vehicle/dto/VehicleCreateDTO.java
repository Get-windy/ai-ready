package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车辆创建/更新 DTO
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "车辆创建/更新请求")
public class VehicleCreateDTO {

    @NotBlank(message = "车牌号不能为空")
    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "车辆品牌")
    private String brand;

    @Schema(description = "车辆型号")
    private String model;

    @Schema(description = "车辆颜色")
    private String color;

    @NotNull(message = "车辆类型不能为空")
    @Schema(description = "车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车")
    private Integer vehicleType;

    @Schema(description = "车辆识别代号(VIN)")
    private String vin;

    @Schema(description = "发动机号")
    private String engineNo;

    @Schema(description = "核定载质量(kg)")
    private BigDecimal ratedLoad;

    @Schema(description = "核定载客人数")
    private Integer ratedPassenger;

    @Schema(description = "车辆尺寸-长(cm)")
    private BigDecimal lengthCm;

    @Schema(description = "车辆尺寸-宽(cm)")
    private BigDecimal widthCm;

    @Schema(description = "车辆尺寸-高(cm)")
    private BigDecimal heightCm;

    @Schema(description = "车厢容积(m³)")
    private BigDecimal cargoVolume;

    @Schema(description = "注册日期")
    private LocalDate registerDate;

    @Schema(description = "运营证号")
    private String operatingPermitNo;

    @Schema(description = "保险到期日")
    private LocalDate insuranceExpireDate;

    @Schema(description = "年检到期日")
    private LocalDate inspectionExpireDate;

    @Schema(description = "保养周期(公里)")
    private Integer maintenanceIntervalKm;

    @Schema(description = "车辆归属：1-公司自有 2-个人自带 3-租赁")
    private Integer ownershipType;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "备注")
    private String remark;
}
