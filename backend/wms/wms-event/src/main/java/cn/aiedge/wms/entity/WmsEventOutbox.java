package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_event_outbox")
@Schema(description = "事件发件箱")
public class WmsEventOutbox extends BaseEntity {
    @Schema(description = "追踪ID")
    private String traceId;
    @Schema(description = "事件类型")
    private String eventType;
    @Schema(description = "来源系统")
    private String sourceSystem;
    @Schema(description = "目标系统")
    private String targetSystem;
    @Schema(description = "事件内容（JSON）")
    private String payload;
    @Schema(description = "状态 0-待发送 1-发送成功 2-发送失败")
    private Integer status;
    @Schema(description = "已重试次数")
    private Integer retryCount;
    @Schema(description = "最大重试次数")
    private Integer maxRetry;
    @Schema(description = "最后一次错误")
    private String lastError;
    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;
}
