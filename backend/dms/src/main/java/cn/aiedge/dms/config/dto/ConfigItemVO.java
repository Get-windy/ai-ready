package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 参数中心列表行（《配送参数开发文档》§3.2 列配置）
 *
 * <p>= 合并后的配置值（全局默认 + 租户覆盖）+ 元数据（类型/默认值/范围/单位/生效方式）+ 校验提示。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数中心行")
public class ConfigItemVO {

    private String configKey;

    @Schema(description = "参数名")
    private String name;

    @Schema(description = "分组键")
    private String group;

    private String groupText;

    @Schema(description = "当前值（敏感键为掩码）")
    private String configValue;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "参数类型：NUMBER/TEXT/BOOLEAN/ENUM/JSON/TIME_RANGE")
    private DmsConfigMeta.ValueType valueType;

    private BigDecimal min;

    private BigDecimal max;

    private String unit;

    private List<DmsConfigMeta.EnumOption> options;

    @Schema(description = "是否允许页面修改")
    private Boolean editable;

    @Schema(description = "生效方式：HOT / RESTART")
    private String effect;

    @Schema(description = "是否敏感（脱敏展示，留空=不修改）")
    private Boolean secret;

    @Schema(description = "敏感键是否已配置（供页面提示）")
    private Boolean configured;

    @Schema(description = "配置说明")
    private String desc;

    @Schema(description = "作用域：TENANT / global")
    private String scope;

    @Schema(description = "当前值是否为租户覆盖（false=继承全局默认）")
    private Boolean tenantOverride;

    private LocalDateTime updateTime;
}
