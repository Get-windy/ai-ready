package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 补能流水查询条件
 */
@Data
@Schema(description = "补能流水查询条件")
public class EnergyLogQuery {

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

    @Schema(description = "主体类型：VEHICLE-四轮车 / RIDER-骑手两轮车；缺省=全部")
    private String subjectType;

    @Schema(description = "车辆ID")
    private Long vehicleId;

    @Schema(description = "骑手ID")
    private Long riderId;

    @Schema(description = "补能类型：1-汽油 2-柴油 3-充电 4-换电 5-加气")
    private Integer energyType;

    @Schema(description = "支付方式：1-现金 2-油卡 3-电卡 4-月租套餐 5-平台代扣")
    private Integer payMode;

    @Schema(description = "是否只看异常记录")
    private Boolean abnormalOnly;

    @Schema(description = "关键字（车牌号 / 配送员姓名 / 站点 / 卡号）")
    private String keyword;

    @Schema(description = "补能日期起（含）")
    private LocalDate startDate;

    @Schema(description = "补能日期止（含）")
    private LocalDate endDate;
}
