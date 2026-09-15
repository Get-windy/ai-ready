package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆档案 DTO（新增 / 修改 / 详情 / 列表统一出参，字段名与《车辆管理开发文档》§2.3 一致）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "车辆档案")
public class VehicleDTO {

    private Long id;

    @Schema(description = "车辆编码（VH 号段，新增时留空由后端生成）")
    private String vehicleCode;

    @NotBlank(message = "车牌号不能为空")
    @Schema(description = "车牌号")
    private String plateNo;

    @NotNull(message = "车辆类型不能为空")
    @Schema(description = "车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车")
    private Integer vehicleType;

    @NotBlank(message = "品牌不能为空")
    private String brand;

    @NotBlank(message = "型号不能为空")
    private String model;

    @NotBlank(message = "颜色不能为空")
    private String color;

    @Schema(description = "车辆识别代号(VIN)，17 位大写字母数字")
    private String vin;

    @NotBlank(message = "发动机号不能为空")
    private String engineNo;

    @NotNull(message = "归属类型不能为空")
    @Schema(description = "归属类型：1-公司自有 2-个人自带 3-租赁")
    private Integer ownershipType;

    @Schema(description = "核定载重(kg)")
    @DecimalMin(value = "0", message = "核定载重不能为负")
    @DecimalMax(value = "99999", message = "核定载重超出上限 99999")
    private BigDecimal ratedLoad;

    @Schema(description = "核定载客(人)")
    @Min(value = 0, message = "核定载客不能为负")
    @Max(value = 99, message = "核定载客超出上限 99")
    private Integer ratedPassenger;

    @Schema(description = "货厢容积(m³)")
    @DecimalMin(value = "0", message = "货厢容积不能为负")
    private BigDecimal cargoVolume;

    @NotNull(message = "注册日期不能为空")
    @Schema(description = "注册日期")
    private LocalDate registerDate;

    @Schema(description = "保养间隔(km)")
    private Integer maintenanceIntervalKm;

    @Schema(description = "营运证号")
    private String operatingPermitNo;

    @Schema(description = "营运证到期日")
    private LocalDate operatingPermitExpireDate;

    @Schema(description = "保险到期日")
    private LocalDate insuranceExpireDate;

    @Schema(description = "年检到期日")
    private LocalDate inspectionExpireDate;

    @Schema(description = "车主姓名（归属类型为个人自带/租赁时填写）")
    private String ownerName;

    @Schema(description = "车主电话")
    private String ownerPhone;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "备注")
    private String remark;

    // ==================== 只读回显字段 ====================

    @Schema(description = "车辆类型文本", accessMode = Schema.AccessMode.READ_ONLY)
    private String vehicleTypeText;

    @Schema(description = "归属类型文本", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownershipTypeText;

    @Schema(description = "车辆状态：0-空闲 1-使用中 2-维修中 3-已报废 4-已出勤", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer status;

    @Schema(description = "状态文本", accessMode = Schema.AccessMode.READ_ONLY)
    private String statusText;

    @Schema(description = "车辆负责人（车队长）ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long vehicleManagerId;

    @Schema(description = "车辆负责人（车队长）姓名", accessMode = Schema.AccessMode.READ_ONLY)
    private String vehicleManagerName;

    @Schema(description = "车辆负责人电话", accessMode = Schema.AccessMode.READ_ONLY)
    private String vehicleManagerPhone;

    @Schema(description = "当前配送员ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long currentRiderId;

    @Schema(description = "当前配送员姓名", accessMode = Schema.AccessMode.READ_ONLY)
    private String currentRiderName;

    @Schema(description = "当前里程(km)", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer currentMileage;

    @Schema(description = "上次保养里程(km)", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer lastMaintenanceKm;

    @Schema(description = "上次保养日期", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate lastMaintenanceDate;

    @Schema(description = "证件最近到期日（保险/年检/营运证三者最小值）", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDate certNearestExpireDate;

    @Schema(description = "证件最近到期剩余天数（负数=已过期；null=未登记任何证件到期日）", accessMode = Schema.AccessMode.READ_ONLY)
    private Long certDaysLeft;

    @Schema(description = "证件到期提示文案（如「保险 12 天后到期」「年检已过期 3 天」）", accessMode = Schema.AccessMode.READ_ONLY)
    private String certWarnText;

    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateTime;
}
