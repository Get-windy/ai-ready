package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 区域分包绑定新增/修改入参
 *
 * <p>线路与配送员均以**档案ID**提交（禁止手输 ID：前端用选择器），
 * 服务端按档案回填编号/名称快照，避免页面传入伪造名称。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "线路-配送员绑定保存入参")
public class RouteRiderSaveDTO {

    @Schema(description = "线路档案ID（erp_route.id）")
    private Long routeId;

    @Schema(description = "配送员ID（dms_rider.id）")
    private Long riderId;

    @Schema(description = "优先级（数值越小越优先，默认 0）")
    private Integer priority;

    @Schema(description = "状态：1-启用 0-停用（默认启用）")
    private Integer status;

    private String remark;
}
