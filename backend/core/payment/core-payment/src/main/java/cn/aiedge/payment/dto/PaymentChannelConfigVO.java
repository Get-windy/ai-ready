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

    /**
     * 该渠道**在本租户下**是否可用。
     *
     * <p>2026-09-21 口径变更：原为「渠道 Bean 的 isAvailable」，而所有渠道实现都无条件返回
     * {@code true} ⇒ **没配任何凭据也显示「可用」**，管理员会以为配好了。
     * 现在改为 {@code 渠道自身可用 && credentialReady} ——
     * 未配置凭据、或凭据格式无效（公钥解析不出来）都算**不可用**。</p>
     */
    @Schema(description = "本租户下是否可用（渠道可用 且 凭据就绪）")
    private Boolean available;

    /**
     * 该渠道的回调验签凭据是否已**就绪且格式有效**。
     *
     * <p>与 {@link #available} 分开返回，是为了让页面能区分
     * 「渠道本身不支持」与「渠道支持但你没配凭据」—— 后者是可修的，需要明确提示。</p>
     *
     * <p>无验签器的渠道（现金/银行转账，不走回调）恒为 {@code true}。</p>
     */
    @Schema(description = "回调验签凭据是否已配置且格式有效")
    private Boolean credentialReady;

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
