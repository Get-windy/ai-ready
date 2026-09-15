package cn.aiedge.dms.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 配送查询-查询条件下拉项
 *
 * <p>用于「配送司机 / 配送车辆 / 送货员 / 制单人」选择器：统一 通用选项结构，
 * 避免前端手输 ID（《配送模块 README》§5 选择器统一）。</p>
 */
@Data
@Schema(description = "配送查询条件下拉项")
public class TaskFilterOptionVO {

    @Schema(description = "选项值（司机ID / 车辆ID；制单人无 ID 时为 null）")
    private Long id;

    @Schema(description = "显示名称（司机姓名 / 车牌号 / 制单人姓名）")
    private String name;

    @Schema(description = "附加信息（手机号 / 车辆编号），用于选择器副标题与检索")
    private String extra;
}
