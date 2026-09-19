package cn.aiedge.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付配置的「配置项」行（微信公众号配置 / 在线退款两个 Tab 的表格行）。
 *
 * <p>字段与取值来源：{@code itemKey} / {@code itemName} / {@code valueType} /
 * {@code description} 来自后端白名单目录 {@code PaymentConfigCatalog}（逐字取自开发文档 §2 的
 * ql361 实测字段清单）；{@code itemValue} / {@code updateTime} 来自 {@code sys_project_config}。</p>
 */
@Data
@Schema(description = "支付配置项")
public class PaymentConfigItemVO {

    @Schema(description = "配置键（sys_project_config.config_key）")
    private String itemKey;

    @Schema(description = "配置项名称")
    private String itemName;

    @Schema(description = "配置值")
    private String itemValue;

    @Schema(description = "值类型：text（文本）/ boolean（开关）/ password（密钥，界面掩码）")
    private String valueType;

    @Schema(description = "说明文案")
    private String description;

    @Schema(description = "配置最后更新时间")
    private LocalDateTime updateTime;
}
