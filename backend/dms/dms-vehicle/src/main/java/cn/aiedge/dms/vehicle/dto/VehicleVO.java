package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆视图对象
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "车辆信息")
public class VehicleVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "车辆编码")
    private String vehicleCode;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "车辆品牌")
    private String brand;

    @Schema(description = "车辆型号")
    private String model;

    @Schema(description = "车辆颜色")
    private String color;

    @Schema(description = "车辆类型")
    private Integer vehicleType;

    @Schema(description = "车辆类型名称")
    private String vehicleTypeName;

    @Schema(description = "核定载质量(kg)")
    private BigDecimal ratedLoad;

    @Schema(description = "车厢容积(m³)")
    private BigDecimal cargoVolume;

    @Schema(description = "保险到期日")
    private LocalDate insuranceExpireDate;

    @Schema(description = "年检到期日")
    private LocalDate inspectionExpireDate;

    @Schema(description = "当前里程(公里)")
    private Integer currentMileage;

    @Schema(description = "当前驾驶员姓名")
    private String currentRiderName;

    @Schema(description = "车辆归属")
    private Integer ownershipType;

    @Schema(description = "车辆归属名称")
    private String ownershipTypeName;

    @Schema(description = "车辆状态")
    private Integer status;

    @Schema(description = "车辆状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
