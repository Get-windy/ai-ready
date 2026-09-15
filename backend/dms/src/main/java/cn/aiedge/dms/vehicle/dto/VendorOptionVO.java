package cn.aiedge.dms.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 维保厂商选择器选项（来源：往来单位 biz_party，供应商 / 其他往来单位）
 *
 * <p>不另建「厂商档案」表：厂商与《资料 → 往来单位》同一口径，避免重复主数据。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "维保厂商（往来单位）选择器选项")
public class VendorOptionVO {

    @Schema(description = "往来单位ID")
    private Long id;

    @Schema(description = "往来单位编码")
    private String code;

    @Schema(description = "往来单位名称")
    private String name;

    @Schema(description = "类型：2-供应商 4-其他往来单位")
    private Integer partyType;

    @Schema(description = "类型名称")
    private String partyTypeText;
}
