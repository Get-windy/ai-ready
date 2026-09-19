package cn.aiedge.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 「场景配置」Tab 的一行 = 一个支付场景（业务场景）× 该场景允许的支付渠道集合。
 *
 * <p>场景清单由后端 {@code PaymentConfigCatalog} 给出，取自 {@code PaymentRequest.bizType}
 * 实体注释里登记的 3 个字面值（SALE_ORDER / TRADE_ORDER / DMS_DELIVERY），
 * <b>不采用 ql361 的场景名</b>（开发文档 §8.3 明确「不承诺在本系统建立同名渠道或同名场景」）。</p>
 *
 * <p>存储载体：{@code sys_project_config} 的 {@code payment.scene.{场景码}}，
 * 值 = 逗号分隔的已启用渠道码（空串表示该场景未启用任何渠道）。</p>
 */
@Data
@Schema(description = "支付场景配置行")
public class PaymentSceneVO {

    @Schema(description = "场景编码（= PaymentRequest.bizType 字面值）")
    private String sceneCode;

    @Schema(description = "场景名称")
    private String sceneName;

    @Schema(description = "该场景已启用的支付渠道码列表")
    private List<String> channels;

    @Schema(description = "配置最后更新时间")
    private LocalDateTime updateTime;
}
