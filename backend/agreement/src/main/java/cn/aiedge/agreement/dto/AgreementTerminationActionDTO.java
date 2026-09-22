package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 终止的对方表态 / 撤回（§13.7）——确认、异议、撤回共用一个请求体。
 *
 * <p>三个动作的区别：</p>
 * <ul>
 *   <li><b>确认</b>：协商一致的终止才需要（PENDING → CONFIRMED，协议随之置为已终止）；</li>
 *   <li><b>异议</b>：待确认时提 = 终止不成立（PENDING → OBJECTED，协议继续有效）；
 *       已终止后提 = 只留痕（**不回滚终止事实**，是否违约另走 §13.8）；
 *       平台清退后的异议即"申诉"（㉝）。</li>
 *   <li><b>撤回</b>：发起方在对方表态前收回（PENDING → WITHDRAWN）。</li>
 * </ul>
 */
@Data
public class AgreementTerminationActionDTO {

    @Schema(description = "异议 / 撤回原因（提异议或撤回时建议填写，会留痕）")
    private String reason;
}
