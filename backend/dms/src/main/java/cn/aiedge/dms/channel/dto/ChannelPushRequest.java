package cn.aiedge.dms.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 渠道派单入参（{@code POST /api/dms/channel/{id}/push-order}）
 *
 * @param taskId 配送任务ID（必填，页面为选择器而非手输）
 *
 * @author AI-Ready Team
 */
@Schema(description = "渠道派单入参")
public record ChannelPushRequest(
        @Schema(description = "配送任务ID") Long taskId) {
}
