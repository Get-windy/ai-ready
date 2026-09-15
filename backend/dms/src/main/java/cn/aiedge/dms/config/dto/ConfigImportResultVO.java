package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数集导入结果（预览 / 落库共用）
 *
 * <p>语义（与《配送参数开发文档》§3.5 一致）：
 * <ul>
 *   <li>逐项校验（键存在性 / 可改 / 类型·范围·枚举 / 敏感键留空跳过），**失败项逐项反馈原因**；</li>
 *   <li>存在失败项时 **APPLY 整体拒绝**（不落任何一项，与批量保存的事务语义一致）；</li>
 *   <li>PREVIEW 只返回差异，不写库、不记审计、不发事件。</li>
 * </ul>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数集导入结果")
public class ConfigImportResultVO {

    @Schema(description = "PREVIEW / APPLY")
    private String mode;

    private Integer total;

    @Schema(description = "将变更（预览）/ 已变更（落库）项数")
    private Integer changed;

    @Schema(description = "值相同无需变更项数")
    private Integer unchanged;

    @Schema(description = "跳过项数（敏感键留空 / 已覆盖且不覆盖）")
    private Integer skipped;

    @Schema(description = "失败项（存在即整体拒绝）")
    private List<Failure> failed = new ArrayList<>();

    @Schema(description = "差异明细（预览与应用一致，便于前端展示 diff）")
    private List<Preview> preview = new ArrayList<>();

    @Data
    @Schema(description = "导入失败项")
    public static class Failure {

        private String configKey;

        private String reason;

        public Failure() {
        }

        public Failure(String configKey, String reason) {
            this.configKey = configKey;
            this.reason = reason;
        }
    }

    @Data
    @Schema(description = "导入差异项")
    public static class Preview {

        private String configKey;

        private String name;

        private String group;

        private String valueType;

        @Schema(description = "变更前值（敏感键为掩码）")
        private String oldValue;

        @Schema(description = "变更后值（敏感键为空=不修改）")
        private String newValue;

        @Schema(description = "动作：CREATE=新增租户覆盖行 / UPDATE=更新租户覆盖行 / SKIP=跳过 / SAME=值相同")
        private String action;

        @Schema(description = "跳过原因")
        private String reason;

        private Boolean secret;
    }
}
