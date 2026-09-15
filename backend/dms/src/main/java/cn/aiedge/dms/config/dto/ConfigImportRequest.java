package cn.aiedge.dms.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 参数集导入请求（《配送参数开发文档》§3.4「导入参数（JSON）」/ §3.5 `POST /config/import`）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "参数集导入请求")
public class ConfigImportRequest {

    @Schema(description = "模式：PREVIEW=仅预览差异不落库 / APPLY=落库（默认）")
    private String mode;

    @Schema(description = "是否覆盖已存在的租户覆盖值（默认 true；false 时已覆盖项跳过）")
    private Boolean overwrite;

    @Schema(description = "导入项（导出物的 items 可直接回灌）")
    private List<ConfigExportVO.Item> items;

    public boolean isPreview() {
        return "PREVIEW".equalsIgnoreCase(mode == null ? "" : mode);
    }

    public boolean isOverwrite() {
        return overwrite == null || overwrite;
    }
}
