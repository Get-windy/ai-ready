package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 维保记录创建 DTO
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "维保记录创建请求")
public class MaintenanceCreateDTO {

    @NotNull(message = "车辆ID不能为空")
    @Schema(description = "关联车辆ID")
    private Long vehicleId;

    @NotNull(message = "维保类型不能为空")
    @Schema(description = "维保类型：1-保养 2-维修 3-年检 4-保险 5-事故 6-其他")
    private Integer maintType;

    @Schema(description = "维保内容描述")
    private String maintContent;

    @Schema(description = "维保费用")
    private BigDecimal maintCost;

    @Schema(description = "维保厂商")
    private String maintVendor;

    @Schema(description = "维保联系人")
    private String maintContact;

    @Schema(description = "维保联系电话")
    private String maintPhone;

    @Schema(description = "维保后里程(公里)")
    private Integer afterMaintMileage;

    @Schema(description = "附件URLs(JSON数组)")
    private String attachmentUrls;

    @Schema(description = "备注")
    private String remark;
}
