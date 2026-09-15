package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 车辆分页查询条件（《车辆管理开发文档》§3.1 金标准查询项）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "车辆分页查询条件")
public class VehicleQueryDTO {

    private Integer pageNum;

    private Integer pageSize;

    @Schema(description = "关键词：车牌号 / 车辆编码 / 品牌 / 型号 / 当前配送员")
    private String keyword;

    @Schema(description = "车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车")
    private Integer vehicleType;

    @Schema(description = "归属类型：1-公司自有 2-个人自带 3-租赁")
    private Integer ownershipType;

    @Schema(description = "车辆状态：0-空闲 1-使用中 2-维修中 3-已报废 4-已出勤")
    private Integer status;

    @Schema(description = "证件到期天数：筛选 N 天内到期（含已过期）的车辆")
    private Integer expiringDays;

    @Schema(description = "证件类型：INSURANCE-保险 INSPECTION-年检 PERMIT-营运证；缺省=任一证件")
    private String certType;

    @Schema(description = "当前配送员姓名（模糊）")
    private String currentRiderName;

    @Schema(description = "所属部门（模糊）")
    private String department;

    @Schema(description = "注册日期（起）")
    private LocalDate registerDateStart;

    @Schema(description = "注册日期（止）")
    private LocalDate registerDateEnd;

    @Schema(description = "排序字段：plateNo / vehicleCode / registerDate / insuranceExpireDate / currentMileage / status / createTime")
    private String sortField;

    @Schema(description = "排序方向：asc / desc，缺省 desc（按创建时间）")
    private String sortOrder;
}
