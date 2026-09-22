package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 协商提案（**反要约**，§13.4）。
 *
 * <p>修改 = 反要约 = 新建一个 DRAFT 版本；一串 DRAFT 版本就是协商时间线。
 * 现行生效版本在协商期间**继续有效**（㉛：谈成之前交易照常）。</p>
 */
@Data
public class AgreementProposalDTO {

    @NotBlank(message = "请填写本次提案的原因：你改了什么、为什么改")
    @Schema(description = "变更原因（正式留痕，进 change_reason）")
    private String changeReason;

    @Schema(description = "协商留言（对话式留痕，进 proposal_note，例如「账期由 30 天改为 45 天，因为旺季资金周转」）")
    private String proposalNote;

    @Schema(description = "以哪一版为基准改；不传 = 当前待双方确认的草稿版本（没有草稿时 = 现行生效版本）")
    private Long baseVersionId;
}
