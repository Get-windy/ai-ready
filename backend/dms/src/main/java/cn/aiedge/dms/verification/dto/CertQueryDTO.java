package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 证照核验（证照维度台账）查询条件
 */
@Data
@Schema(description = "证照核验查询条件")
public class CertQueryDTO {

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

    @Schema(description = "配送员姓名（模糊）")
    private String riderName;

    @Schema(description = "配送员ID")
    private Long riderId;

    @Schema(description = "证照类型：1-驾驶证 2-行驶证 3-健康证 4-从业资格证 5-其他")
    private Integer certType;

    @Schema(description = "证照状态：0-待核验 1-有效 2-已过期 3-无效")
    private Integer verifyStatus;

    @Schema(description = "是否只看即将到期（提醒期内）或已过期")
    private Boolean expiring;
}
