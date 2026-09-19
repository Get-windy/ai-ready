package cn.aiedge.platform.dto;

/**
 * 「平台配置 → 测试连接」的统一返回。
 *
 * <p>为什么要专门包一层：改造前三个 {@code testConnection} 都只 {@code return true}
 * （不建连接、不校验凭据），页面点「测试连接」永远成功，用户无法区分
 * 「真的通了」与「压根没测」。故改为返回**布尔 + 人话原因**，
 * 前端弹窗直接把 {@code message} 展示出来，失败时能看到是「主机不通」「认证失败」
 * 还是「配置缺字段」。
 *
 * @param success 是否通过
 * @param message 结果说明（对用户可见，必须是可执行的中文，不要塞异常堆栈）
 */
public record ConnectionTestResult(boolean success, String message) {

    public static ConnectionTestResult ok(String message) {
        return new ConnectionTestResult(true, message);
    }

    public static ConnectionTestResult fail(String message) {
        return new ConnectionTestResult(false, message);
    }
}
