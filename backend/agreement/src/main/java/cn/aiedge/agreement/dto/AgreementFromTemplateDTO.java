package cn.aiedge.agreement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 「从模板发起契约」的请求体。
 *
 * <p>发起后会：① 建协议主档与首个**草稿**版本（复用 {@code AgreementService#create}，
 * 编号走系统号段）；② 把模板的设定 / 条款 / 文字**预填**到这个草稿版本上；
 * ③ 把**模板来源**写进版本快照（"基于模板 X，第 N 版"）—— 司法可追溯（§13.9）。</p>
 *
 * <p>⚠️ 预填 ≠ 已约定：草稿仍需双方确认、双签之后才能生效；
 * 生效设定一律由 {@code AgreementRuntime} 从"双方签署的那一版"读取，**绝不读模板**。</p>
 */
@Data
public class AgreementFromTemplateDTO {

    /** 甲方主体（往来单位）ID */
    @NotNull(message = "请选择甲方主体")
    private Long partyAId;

    @NotNull(message = "请选择甲方所属租户")
    private Long partyATenantId;

    /** 乙方主体；消费者单方承诺时为「不特定消费者」，可空 */
    private Long partyBId;

    private Long partyBTenantId;

    @NotBlank(message = "请填写协议标题")
    private String title;

    /** 协议类型；不传则取模板上登记的类型（模板必填类型，故最终一定有值） */
    private String agreementType;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
