package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配置项元数据（《配送参数开发文档》§3.1：元数据驱动渲染与校验）
 *
 * <p>由 {@code DmsConfigMetaRegistry} 与代码同源定义，前端只按 {@link #valueType} 渲染控件、不做业务判断。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配置项元数据")
public class DmsConfigMeta {

    /** 参数类型（决定前端控件与后端校验） */
    public enum ValueType {
        /** 数字：带 min/max/unit */
        NUMBER,
        /** 文本 */
        TEXT,
        /** 布尔开关 */
        BOOLEAN,
        /** 枚举：options 内取值 */
        ENUM,
        /** JSON：需可解析 */
        JSON,
        /** 时间范围：HH:mm-HH:mm[(:ss)] */
        TIME_RANGE
    }

    @Schema(description = "分组键")
    private String group;

    @Schema(description = "分组名")
    private String groupText;

    @Schema(description = "参数键")
    private String configKey;

    @Schema(description = "参数名")
    private String name;

    @Schema(description = "参数类型")
    private ValueType valueType;

    @Schema(description = "默认值（「恢复默认」依据）")
    private String defaultValue;

    @Schema(description = "最小值（NUMBER）")
    private BigDecimal min;

    @Schema(description = "最大值（NUMBER）")
    private BigDecimal max;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "枚举项（ENUM）")
    private List<EnumOption> options;

    @Schema(description = "是否允许页面修改")
    private Boolean editable;

    @Schema(description = "生效方式：HOT-保存即热生效 / RESTART-需重启")
    private String effect;

    @Schema(description = "是否敏感（密钥/令牌/密码：脱敏展示、留空=不修改）")
    private Boolean secret;

    @Schema(description = "说明")
    private String desc;

    @Data
    @Schema(description = "枚举项")
    public static class EnumOption {
        private String label;
        private String value;
    }
}
