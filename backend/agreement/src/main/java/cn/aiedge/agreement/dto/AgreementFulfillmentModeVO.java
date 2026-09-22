package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 履约方式的一项（下发前端）。
 *
 * <p>⚠️ 返回值是**集合**：同一份协议里"同城直发 + 异地中转"可以并存（§13.10）。
 * 前端不要把它渲染成单选控件。</p>
 */
@Data
public class AgreementFulfillmentModeVO {

    /** DROP_SHIP 直发 / TRANSIT_STOCK 中转 / PICKUP 自提 / LOCAL_STOCK 自有库存 */
    private String mode;

    private String modeLabel;

    /** 适用范围说明（人读，不参与自动执行） */
    private String scopeNote;

    private Integer sort;
}
