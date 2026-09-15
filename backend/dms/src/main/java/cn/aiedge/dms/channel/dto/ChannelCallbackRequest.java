package cn.aiedge.dms.channel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 外部平台回调入参（《渠道管理开发文档》§3.3 {@code POST /api/dms/channel/callback}）
 *
 * <p><b>签名口径</b>（适配器接入方按此实现，与 {@code ChannelOrderService#verifySign} 一一对应）：</p>
 * <pre>
 *   signBase = channelCode + timestamp + nonce + channelOrderNo + status
 *   sign     = HEX( HMAC-SHA256( appSecret 或 signKey, signBase ) )   // 大小写不敏感
 * </pre>
 * <p>时间戳支持秒/毫秒（&lt; 1e12 视为秒）；容差由配置 {@code dms.channel.callback.tolerance.seconds} 控制。</p>
 *
 * @author AI-Ready Team
 */
@Data
@Schema(description = "渠道回调入参")
public class ChannelCallbackRequest {

    @Schema(description = "渠道编码（必填）")
    private String channelCode;

    @Schema(description = "请求时间戳（秒或毫秒）")
    private Long timestamp;

    @Schema(description = "请求号（防重放，建议 UUID）")
    private String nonce;

    @Schema(description = "签名（HMAC-SHA256 十六进制）")
    private String sign;

    @Schema(description = "租户ID（同一渠道编码跨租户重名时必填）")
    private Long tenantId;

    @Schema(description = "事件类型，如 rider_accepted / order_finished")
    private String eventType;

    @Schema(description = "外部平台单号")
    private String channelOrderNo;

    @Schema(description = "平台侧状态原文：ACCEPTED/DELIVERING/COMPLETED/CANCELLED/EXCEPTION")
    private String status;

    @Schema(description = "平台配送员姓名（回写任务快照）")
    private String riderName;

    @Schema(description = "平台配送员电话（回写任务快照）")
    private String riderPhone;

    @Schema(description = "骑手纬度")
    private BigDecimal lat;

    @Schema(description = "骑手经度")
    private BigDecimal lng;

    @Schema(description = "本系统任务编号（新单场景用于关联任务）")
    private String taskNo;

    @Schema(description = "原始报文（留痕，截断 2000 字符）")
    private String payload;
}
