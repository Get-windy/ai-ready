package cn.aiedge.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 「支付方式」Tab 的一行 = 一个支付渠道 + 该渠道**已落库**的参数。
 *
 * <p>渠道清单与单笔限额来自 Spring 容器内的 {@code PaymentChannel} Bean（不是数据库行，
 * 见开发文档 §1.1 / §5.2）；参数部分来自 {@code sys_project_config}。
 * 两者在此 VO 合并，使列表页能直接展示「参数是否已配置」，无需逐行再发一次请求。</p>
 *
 * <p><b>密钥不在列表回传</b>：{@code appSecret} 只由单渠道详情端点返回，
 * 列表只给 {@code secretConfigured} 布尔（避免密钥在列表接口里明文扩散）。
 * 编辑抽屉打开时走详情端点取原值。见开发文档 §8.2「凭据的密钥保护」。</p>
 */
@Data
@Schema(description = "支付渠道配置行")
public class PaymentChannelConfigVO {

    @Schema(description = "渠道编码：ALIPAY / WECHAT / UNIONPAY / BANK / CASH")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "单笔最低限额（元）")
    private BigDecimal minAmount;

    @Schema(description = "单笔最高限额（元）")
    private BigDecimal maxAmount;

    @Schema(description = "渠道是否可用（由渠道 Bean 的 isAvailable 决定）")
    private Boolean available;

    @Schema(description = "APP ID")
    private String appId;

    @Schema(description = "商户号")
    private String merchantNo;

    @Schema(description = "异步通知 URL")
    private String notifyUrl;

    @Schema(description = "是否已配置 API 密钥（密钥原文不在列表回传）")
    private Boolean secretConfigured;

    @Schema(description = "是否启用该渠道")
    private Boolean enabled;

    @Schema(description = "配置最后更新时间")
    private LocalDateTime updateTime;
}
