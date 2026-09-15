package cn.aiedge.dms.verification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 实名认证台账查询条件
 */
@Data
@Schema(description = "实名认证台账查询条件")
public class KycQueryDTO {

    @Schema(description = "页码（从 1 开始）")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

    @Schema(description = "配送员姓名（模糊）")
    private String riderName;

    @Schema(description = "配送员手机号（模糊）")
    private String riderPhone;

    @Schema(description = "配送员类型：1-企业员工 2-众包兼职 3-外部平台配送员 4-社会车辆司机")
    private Integer riderType;

    @Schema(description = "认证状态：0-待提交 1-待审核 2-已通过 3-已驳回 4-已过期")
    private Integer verifyStatus;

    @Schema(description = "外部平台渠道ID")
    private Long channelId;

    @Schema(description = "是否只看有到期预警的（true=证照/背书在提醒期内或已过期）")
    private Boolean expiring;

    @Schema(description = "是否只看具备接单资质的（true=认证通过且证照有效）")
    private Boolean eligible;

    @Schema(description = "创建日期起（含）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "创建日期止（含）")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
