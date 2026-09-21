package cn.aiedge.base.payment;

/**
 * 支付回调验签失败。
 *
 * <p>抛出即代表**不得继续处理该回调**：不落支付状态、不改订单。
 * 调用方应返回渠道要求的「失败应答」，让网关按自己的策略重推或转对账。</p>
 *
 * <p>刻意继承 {@link RuntimeException} 而非受检异常：验签是必经关卡，
 * 不希望在调用链上被 {@code catch (Exception)} 顺手吞掉后继续走成功分支。</p>
 */
public class PaymentCallbackVerificationException extends RuntimeException {

    public PaymentCallbackVerificationException(String message) {
        super(message);
    }

    public PaymentCallbackVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
