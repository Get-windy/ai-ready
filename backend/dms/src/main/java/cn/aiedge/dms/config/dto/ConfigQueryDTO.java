package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 参数中心查询条件（《配送参数开发文档》§3.3）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数中心查询条件")
public class ConfigQueryDTO {

    private Integer pageNum;

    private Integer pageSize;

    @Schema(description = "关键词：参数键 / 参数名 / 说明")
    private String keyword;

    @Schema(description = "分组键：DISPATCH/TRACKING/SIGN/SETTLEMENT/VERIFICATION/ENERGY/MAP/OTHER")
    private String group;

    @Schema(description = "参数类型：NUMBER/TEXT/BOOLEAN/ENUM/JSON/TIME_RANGE")
    private String valueType;

    @Schema(description = "是否可改")
    private Boolean editable;

    @Schema(description = "生效方式：HOT / RESTART")
    private String effect;

    @Schema(description = "仅看已配置（值非空）")
    private Boolean configuredOnly;

    @Schema(description = "作用域：TENANT / GLOBAL")
    private String scope;

    @Schema(description = "仅看租户覆盖（true）/ 仅看继承全局（false）")
    private Boolean tenantOverride;

    @Schema(description = "更新时间起（yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd）")
    private String updateTimeStart;

    @Schema(description = "更新时间止（yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd，含当天）")
    private String updateTimeEnd;
}
