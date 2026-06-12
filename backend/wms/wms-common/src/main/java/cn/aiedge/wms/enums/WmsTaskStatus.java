package cn.aiedge.wms.enums;

/**
 * WMS 任务状态常量定义.
 * <p>使用 final class + private 构造方法替代 interface 常量模式（常量接口反模式），
 * 保证状态常量的类型安全且不被实现污染。</p>
 */
public final class WmsTaskStatus {

    private WmsTaskStatus() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** 待处理 */
    public static final int PENDING = 0;
    /** 处理中 */
    public static final int IN_PROGRESS = 1;
    /** 已完成 */
    public static final int COMPLETED = 2;
    /** 已取消 */
    public static final int CANCELLED = 3;
    /** 异常 */
    public static final int EXCEPTION = 4;

    /**
     * 校验状态值是否合法.
     * @param status 状态值
     * @return true 如果状态在合法范围内
     */
    public static boolean isValid(int status) {
        return status >= PENDING && status <= EXCEPTION;
    }

    /**
     * 获取状态描述.
     * @param status 状态值
     * @return 中文描述，未知状态返回 "未知"
     */
    public static String getText(int status) {
        return switch (status) {
            case PENDING -> "待处理";
            case IN_PROGRESS -> "处理中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            case EXCEPTION -> "异常";
            default -> "未知";
        };
    }
}
