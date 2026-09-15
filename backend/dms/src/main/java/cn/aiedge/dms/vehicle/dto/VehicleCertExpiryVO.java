package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 车辆证件到期清单行（《车辆管理开发文档》§3.4 `/vehicle/expiring`）
 *
 * 一行 = 一辆车的一个证件（保险 / 年检 / 营运证），已过期在前、按剩余天数升序。
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "车辆证件到期清单行")
public class VehicleCertExpiryVO {

    private Long vehicleId;

    private String vehicleCode;

    private String plateNo;

    @Schema(description = "证件类型：INSURANCE-保险 INSPECTION-年检 PERMIT-营运证")
    private String certType;

    private String certTypeText;

    @Schema(description = "证件到期日")
    private LocalDate certDate;

    @Schema(description = "剩余天数（负数=已过期）")
    private Long daysLeft;

    @Schema(description = "告警级别：EXPIRED-已过期 / WARNING-临近到期 / NORMAL-正常")
    private String warnLevel;

    private Integer status;

    private String statusText;

    private String currentRiderName;

    private String ownershipTypeText;

    private String department;
}
