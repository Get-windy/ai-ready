package cn.aiedge.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应 VO
 * 与前端 src/api/notification.ts 的 NotificationInfo 对齐
 */
@Data
@Schema(description = "通知信息")
public class NotificationVO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知类型 1-系统通知 2-业务通知 3-审批通知")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "摘要（截取前200字符）")
    private String summary;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "已读状态 0-未读 1-已读")
    private Integer readStatus;
}
