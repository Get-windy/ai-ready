package cn.aiedge.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 未读数量统计 VO
 * 与前端 src/api/notification.ts 的 UnreadCount 对齐
 */
@Data
@Schema(description = "未读数量统计")
public class UnreadCountVO {

    @Schema(description = "总未读")
    private long total;

    @Schema(description = "系统通知未读")
    private long system;

    @Schema(description = "业务通知未读")
    private long business;

    @Schema(description = "审批通知未读")
    private long approval;

    public UnreadCountVO() {}

    public UnreadCountVO(long total, long system, long business, long approval) {
        this.total = total;
        this.system = system;
        this.business = business;
        this.approval = approval;
    }
}
