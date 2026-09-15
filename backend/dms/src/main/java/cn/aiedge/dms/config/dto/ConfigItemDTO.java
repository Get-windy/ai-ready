package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 参数保存项（单条 / 批量共用）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数保存项")
public class ConfigItemDTO {

    @Schema(description = "配置键")
    private String configKey;

    /**
     * 目标值。
     * ⚠️ 敏感键（密钥/令牌/密码）：**留空视为不修改**（页面拿到的是掩码）；需要清空请用 `/{key}` DELETE。
     */
    @Schema(description = "目标值（敏感键留空=不修改）")
    private String configValue;
}
