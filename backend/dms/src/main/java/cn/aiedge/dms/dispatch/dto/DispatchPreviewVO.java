package cn.aiedge.dms.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 自动调度预览（《智能调度开发文档》§3.3 `/auto/preview`：**不落库**，先看分配结果再确认执行）
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "自动调度预览结果")
public class DispatchPreviewVO {

    @Schema(description = "本次生效策略：NEAREST/BALANCED/SCORE/AREA")
    private String strategy;

    private String strategyText;

    @Schema(description = "扫描到的待分配任务数")
    private int taskCount;

    @Schema(description = "可成功指派的任务数")
    private int assignableCount;

    @Schema(description = "无法指派的任务数（无人可派/超限/离线）")
    private int unassignableCount;

    @Schema(description = "逐单分配结果（含命中规则与理由）")
    private List<Row> rows;

    @Data
    @Schema(description = "单条任务的分配预览")
    public static class Row {

        private Long taskId;

        private String taskNo;

        private String customerName;

        private Integer status;

        @Schema(description = "预估重量(kg)/体积(m³)，用于超限约束展示")
        private java.math.BigDecimal totalWeight;

        private java.math.BigDecimal totalVolume;

        @Schema(description = "命中的配送员（null=无人可派）")
        private Long riderId;

        private String riderName;

        private String riderPhone;

        private java.math.BigDecimal distanceMeters;

        @Schema(description = "命中规则说明，如「最近可用：张三 340 米、在途 1 单」")
        private String ruleHit;

        @Schema(description = "未命中/被拒原因")
        private String failReason;

        @Schema(description = "是否可指派")
        private Boolean assignable;

        // ── 渠道派单（任务指定了运力渠道且渠道可用时，走外部平台下单，不占本系统配送员）──

        @Schema(description = "命中的运力渠道ID（null=走自有运力派单）")
        private Long channelId;

        @Schema(description = "命中的运力渠道名称")
        private String channelName;

        @Schema(description = "是否经外部渠道派单")
        private Boolean viaChannel;

        @Schema(description = "外部平台单号（执行后回填）")
        private String channelOrderNo;
    }
}
