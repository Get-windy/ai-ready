package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发起终止（§13.7）。
 *
 * <p>⚠️ 请记住本模块的口径：**终止 ≠ 免责**。发起终止只是记录「停止履行」这个事实，
 * 系统不会自动结清、不会自动免责。</p>
 */
@Data
public class AgreementTerminationCreateDTO {

    @NotBlank(message = "请选择终止来源")
    @Schema(description = "MUTUAL_AGREEMENT 协商一致 / NATURAL_EXPIRY 自然到期 / "
            + "UNILATERAL 单方终止 / COUNTERPARTY_BREACH 因对方违约 / PLATFORM_EXPULSION 平台清退")
    private String source;

    @NotBlank(message = "请填写终止依据：引用协议条款或法定情形")
    @Schema(description = "终止依据（必填）：如「按第 2 版第 7 条，逾期付款超过 15 日」或法定情形")
    private String basisText;

    @Schema(description = "是否主张对方违约（⚠️ 只是当事人的主张，平台不认定违约、不判赔多少）")
    private Boolean claimCounterpartyBreach;

    @Schema(description = "主张违约的具体说明（主张违约时建议填写）")
    private String breachNote;

    @Schema(description = "从何时起停止履行（不传 = 立即）。⚠️ 只是停止履行的事实，不含结清与免责")
    private LocalDateTime stopPerformanceAt;
}
