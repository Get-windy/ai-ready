package cn.aiedge.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知查询参数
 * 与前端 src/api/notification.ts 的 NotificationQuery 对齐
 */
@Data
@Schema(description = "通知查询参数")
public class NotificationQuery {

    @Schema(description = "通知类型 1-系统通知 2-业务通知 3-审批通知")
    private Integer type;

    @Schema(description = "已读状态 0-未读 1-已读")
    private Integer readStatus;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
