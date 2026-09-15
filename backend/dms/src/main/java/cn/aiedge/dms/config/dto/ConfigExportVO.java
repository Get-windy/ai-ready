package cn.aiedge.dms.config.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 参数集导出（《配送参数开发文档》§3.4「导出参数清单」/ §3.5 `GET /config/export`）
 *
 * <p>导出物是**可直接回灌**的 JSON 参数集：`{schema, exportedAt, tenantId, count, items:[…]}`，
 * 该结构原样作为 `POST /config/import` 的请求体（`items` 即导入项）。</p>
 *
 * <p>⚠️ 敏感键（密钥/令牌/密码）**不导出明文**：`configValue` 置空、`secret=true`、`configured` 标记是否已配置，
 * 回灌时按「敏感键留空=不修改」语义跳过，避免密钥经由导出物泄漏。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数集导出物")
public class ConfigExportVO {

    @Schema(description = "结构版本，导入时校验")
    private String schema = "dms-config-export/v1";

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportedAt;

    @Schema(description = "导出所用的租户（合并口径：全局默认 + 该租户覆盖）")
    private Long tenantId;

    @Schema(description = "导出项数")
    private Integer count;

    private List<Item> items;

    @Data
    @Schema(description = "导出/导入项")
    public static class Item {

        private String configKey;

        @Schema(description = "参数名（元数据）")
        private String name;

        private String group;

        private DmsConfigMeta.ValueType valueType;

        private String unit;

        @Schema(description = "参数值；敏感键为空（不导出明文）")
        private String configValue;

        @Schema(description = "是否敏感键")
        private Boolean secret;

        @Schema(description = "敏感键是否已配置")
        private Boolean configured;

        @Schema(description = "元数据默认值")
        private String defaultValue;
    }
}
