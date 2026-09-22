package cn.aiedge.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新建协议草稿的请求体（契约见 DOMAIN-MODEL §十二 与前端 {@code api/agreement.ts}）。
 *
 * <p>{@code agreementType} 用**枚举名**（PLATFORM_SERVICE / DISTRIBUTION / GOODS_FRAMEWORK /
 * CONSUMER_PROMISE），不是数字码 —— 前端与接口都按名字交互，DB 里存字符串。</p>
 */
@Data
public class AgreementCreateDTO {

    @NotBlank(message = "请选择协议类型")
    private String agreementType;

    /** 甲方主体（往来单位）ID */
    @NotNull(message = "请选择甲方主体")
    private Long partyAId;

    /** 甲方所属租户 ID（两端对称，裁定②） */
    @NotNull(message = "请选择甲方所属租户")
    private Long partyATenantId;

    /** 乙方主体；消费者单方承诺时可为空（乙方为不特定消费者） */
    private Long partyBId;

    /** 乙方所属租户；消费者单方承诺时可为空 */
    private Long partyBTenantId;

    @NotBlank(message = "请填写协议标题")
    private String title;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
