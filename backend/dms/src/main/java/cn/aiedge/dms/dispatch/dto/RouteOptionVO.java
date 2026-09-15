package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 线路档案选项（区域分包绑定的「线路」选择器数据源）
 *
 * <p>来源：《资料 → 配送管理 → 线路》线路档案 `erp_route`（DMS 侧只读查询，不复制主数据）。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "线路档案选项")
public class RouteOptionVO {

    private Long id;

    @Schema(description = "线路编号")
    private String routeCode;

    @Schema(description = "线路名称")
    private String routeName;

    @Schema(description = "线路类型-自配 0/1")
    private Integer routeSelf;

    @Schema(description = "线路类型-物流 0/1")
    private Integer routeLogistics;

    @Schema(description = "物流公司名称")
    private String expressName;
}
