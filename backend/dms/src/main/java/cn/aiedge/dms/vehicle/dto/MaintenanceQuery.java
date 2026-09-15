package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 维保记录查询条件（金标准：车辆车牌联查 + 多类型 + 日期/费用区间 + 厂商 + 是否含附件）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "维保记录查询条件")
public class MaintenanceQuery {

    @Schema(description = "车辆ID")
    private Long vehicleId;

    @Schema(description = "车辆车牌号（模糊联查）")
    private String plateNo;

    @Schema(description = "维保单号（模糊）")
    private String maintNo;

    @Schema(description = "维保类型多选：1-保养 2-维修 3-年检 4-保险 5-事故 6-其他")
    private List<Integer> maintTypes;

    @Schema(description = "维保日期起")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @Schema(description = "维保日期止")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    @Schema(description = "费用最小值")
    private BigDecimal costMin;

    @Schema(description = "费用最大值")
    private BigDecimal costMax;

    @Schema(description = "维保厂商（名称模糊，含未建档厂商）")
    private String vendor;

    @Schema(description = "维保厂商往来单位ID（精确过滤已建档厂商）")
    private Long vendorId;

    @Schema(description = "是否含附件：1-仅含附件 0-仅不含附件 空-全部")
    private Integer hasAttachment;
}
