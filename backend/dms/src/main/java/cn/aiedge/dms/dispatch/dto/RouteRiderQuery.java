package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域分包绑定查询条件
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "线路-配送员绑定查询条件")
public class RouteRiderQuery {

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 20;

    @Schema(description = "线路档案ID")
    private Long routeId;

    @Schema(description = "配送员ID")
    private Long riderId;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Schema(description = "关键字（线路编号/名称、配送员姓名/手机号）")
    private String keyword;
}
