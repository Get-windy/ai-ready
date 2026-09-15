package cn.aiedge.dms.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 实名认证 / 资质提交请求
 *
 * <p>自有员工（riderType=1）走企业自审：提交身份信息 + 证照明细即可；
 * 外部平台配送员（riderType≥2）由渠道方背书：{@code endorseOrg / endorseResult / endorseExpireDate}
 * 记录背书结论与有效期，不重复采集内部证件细节。</p>
 */
@Data
@Schema(description = "实名认证提交请求")
public class KycSubmitDTO {

    @Schema(description = "台账ID（修改时传；为空则新增）")
    private Long id;

    @NotNull(message = "配送员不能为空")
    @Schema(description = "配送员ID（dms_rider.id）")
    private Long riderId;

    @Schema(description = "证件姓名")
    private String realName;

    @Schema(description = "身份证号原文（服务端脱敏后落库，不存原文）")
    private String idCardNo;

    @Schema(description = "身份证人像面 URL")
    private String idCardFrontUrl;

    @Schema(description = "身份证国徽面 URL")
    private String idCardBackUrl;

    @Schema(description = "背书渠道 / 背景审查机构（外部平台由渠道方提供）")
    private String endorseOrg;

    @Schema(description = "背书 / 背景审查结论：1-通过 0-未通过")
    private Integer endorseResult;

    @Schema(description = "背书 / 资质有效期")
    private LocalDate endorseExpireDate;

    @Schema(description = "证照明细")
    private List<CertificateDTO> certificates;

    @Schema(description = "备注")
    private String remark;

    /**
     * 提交后是否直接进入审核队列（false=仅存草稿）
     */
    @Schema(description = "是否提交审核：true-提交（状态置待审核）false-存草稿")
    private Boolean submitAudit = Boolean.TRUE;

    @Data
    @Schema(description = "证照明细")
    public static class CertificateDTO {

        @Schema(description = "证照类型：1-驾驶证 2-行驶证 3-健康证 4-从业资格证 5-其他")
        private Integer certType;

        @Schema(description = "证照编号")
        private String certNo;

        @Schema(description = "发证日期")
        private LocalDate issueDate;

        @Schema(description = "有效期至")
        private LocalDate expireDate;

        @Schema(description = "证照影像 URL")
        private String certUrl;

        @Schema(description = "备注")
        private String remark;
    }
}
