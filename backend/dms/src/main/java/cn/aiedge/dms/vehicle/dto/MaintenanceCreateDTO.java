package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 维保记录创建/更新 DTO
 *
 * <p>⚠️ 2026-09-12 金标准修复：原 DTO 缺 {@code maintDate}，前端提交的维保日期被静默丢弃
 * （记录永远落库为当天），本次补齐并作为可空字段由 Service 兜底为当天。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "维保记录创建/更新请求")
public class MaintenanceCreateDTO {

    @NotNull(message = "车辆ID不能为空")
    @Schema(description = "关联车辆ID")
    private Long vehicleId;

    @NotNull(message = "维保类型不能为空")
    @Schema(description = "维保类型：1-保养 2-维修 3-年检 4-保险 5-事故 6-其他")
    private Integer maintType;

    @Schema(description = "维保日期（缺省为当天）")
    private LocalDate maintDate;

    @Schema(description = "维保内容描述")
    private String maintContent;

    @Schema(description = "维保费用")
    private BigDecimal maintCost;

    @Schema(description = "维保厂商/服务商")
    private String maintVendor;

    @Schema(description = "维保厂商往来单位ID（biz_party.id，供应商/其他往来单位）；填写后由服务端按档案回填 maintVendor 名称快照，留空则按 maintVendor 手工名记账")
    private Long vendorId;

    @Schema(description = "维保联系人")
    private String maintContact;

    @Schema(description = "维保联系电话")
    private String maintPhone;

    @Schema(description = "维保后里程(公里)")
    private Integer afterMaintMileage;

    @Schema(description = "下次维保日期（留空按维保类型自动推算：保养+90天 / 年检·保险+1年）")
    private LocalDate nextMaintDate;

    @Schema(description = "下次维保提醒里程(公里)（留空按「维保后里程 + 车辆保养间隔」自动推算，仅保养）")
    private Integer nextMaintMileage;

    @Schema(description = "附件URLs(JSON数组)")
    private String attachmentUrls;

    @Schema(description = "备注")
    private String remark;
}
