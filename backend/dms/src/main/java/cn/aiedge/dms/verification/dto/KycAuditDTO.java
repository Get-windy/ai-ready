package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 实名认证审核请求
 */
@Data
@Schema(description = "实名认证审核请求")
public class KycAuditDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：true-通过 false-驳回")
    private Boolean approved;

    @Schema(description = "审核意见（驳回时必填）")
    private String auditRemark;

    @Schema(description = "背书 / 资质有效期（审核通过时可核定）")
    private LocalDate endorseExpireDate;

    @Schema(description = "审核时核定/修正的证照明细（传则整体替换）")
    private List<KycSubmitDTO.CertificateDTO> certificates;
}
