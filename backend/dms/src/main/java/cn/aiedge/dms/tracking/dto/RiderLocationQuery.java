package cn.aiedge.dms.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 配送员实时位置查询条件（《实时跟踪开发文档》§3.2）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送员实时位置查询条件")
public class RiderLocationQuery {

    private Integer pageNum;

    private Integer pageSize;

    @Schema(description = "关键词：配送员姓名 / 手机号")
    private String keyword;

    @Schema(description = "配送员档案状态：0-离线 1-空闲 2-忙碌 3-休息")
    private Integer status;

    @Schema(description = "仅看在线（最近上报在阈值内）")
    private Boolean onlineOnly;
}
