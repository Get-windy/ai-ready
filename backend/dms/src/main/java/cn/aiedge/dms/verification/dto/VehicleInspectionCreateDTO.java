package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 验车记录创建 DTO
 */
@Data
@Schema(description = "验车记录创建请求")
public class VehicleInspectionCreateDTO {

    @NotNull(message = "车辆ID不能为空")
    @Schema(description = "车辆ID")
    private Long vehicleId;

    @Schema(description = "验车类型：1-出车验车 2-收车验车")
    private Integer inspectionType;

    @Schema(description = "外观状况：0-正常 1-异常")
    private Integer exteriorStatus;

    @Schema(description = "外观异常描述")
    private String exteriorRemark;

    @Schema(description = "外观照片URLs(JSON数组)")
    private String exteriorPhotos;

    @Schema(description = "轮胎状况：0-正常 1-异常")
    private Integer tireStatus;

    @Schema(description = "轮胎异常描述")
    private String tireRemark;

    @Schema(description = "灯光状况：0-正常 1-异常")
    private Integer lightStatus;

    @Schema(description = "灯光异常描述")
    private String lightRemark;

    @Schema(description = "刹车状况：0-正常 1-异常")
    private Integer brakeStatus;

    @Schema(description = "刹车异常描述")
    private String brakeRemark;

    @Schema(description = "清洁状况：0-正常 1-异常")
    private Integer cleanlinessStatus;

    @Schema(description = "清洁异常描述")
    private String cleanlinessRemark;

    @Schema(description = "当前里程")
    private Integer mileage;

    @Schema(description = "油量/电量百分比")
    private Integer fuelLevel;

    @Schema(description = "灭火器：0-正常 1-缺失/过期")
    private Integer fireExtinguisher;

    @Schema(description = "三角警示牌：0-有 1-缺失")
    private Integer warningTriangle;

    @Schema(description = "备注")
    private String remark;
}
