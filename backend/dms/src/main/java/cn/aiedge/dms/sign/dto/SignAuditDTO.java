package cn.aiedge.dms.sign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 签收审核请求（《签收管理开发文档》§3.4 审核流转）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "签收审核请求")
public class SignAuditDTO {

    @NotNull(message = "审核结论不能为空")
    @Schema(description = "审核结论：1-通过 2-驳回")
    private Integer auditStatus;

    @Schema(description = "审核意见（驳回时必填，说明驳回原因）")
    private String auditRemark;
}
