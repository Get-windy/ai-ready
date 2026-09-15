package cn.aiedge.dms.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配送渠道出参
 *
 * 说明：
 * 1. {@code configJson} 已由 Service 脱敏（AppSecret/token/password 等值替换为 ******），
 *    避免前端与日志侧泄露对接凭据；
 * 2. 在线运力不落冗余列，按 {@code dms_rider.channel_id} 实时统计（见《渠道管理开发文档》§3.1）。
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "配送渠道")
public class ChannelVO {

    private Long id;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "类型：1-自有员工 2-众包兼职 3-外部平台 4-社会车辆")
    private Integer channelType;

    @Schema(description = "适配器 Bean 名称")
    private String adapterBean;

    @Schema(description = "对接配置 JSON（已脱敏）")
    private String configJson;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "调度优先级（越小越优先）")
    private Integer priority;

    @Schema(description = "对接状态：0-未对接 1-已对接 2-对接异常")
    private Integer linkStatus;

    @Schema(description = "最近连通性测试时间")
    private LocalDateTime lastTestTime;

    @Schema(description = "最近连通性测试结果摘要")
    private String lastTestResult;

    @Schema(description = "覆盖区域（行政区划名称，逗号分隔）")
    private String coverageArea;

    @Schema(description = "计费方式：1-按单 2-按距 3-按重")
    private Integer billingType;

    @Schema(description = "计费规则 JSON")
    private String billingConfig;

    @Schema(description = "备注")
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ── 实时统计（不落库） ──

    @Schema(description = "该渠道配送员总数")
    private Integer riderTotal;

    @Schema(description = "该渠道在线配送员数（空闲/忙碌）")
    private Integer riderOnline;
}
