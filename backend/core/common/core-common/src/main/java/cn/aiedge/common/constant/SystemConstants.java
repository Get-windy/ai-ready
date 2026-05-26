package cn.aiedge.common.constant;

/**
 * 系统常量定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public final class SystemConstants {

    /**
     * 默认租户ID
     */
    public static final Long DEFAULT_TENANT_ID = 1L;

    /**
     * 状态常量
     */
    public static final class Status {
        public static final Integer NORMAL = 0;
        public static final Integer DISABLED = 1;
        public static final Integer LOCKED = 2;
        public static final Integer DELETED = 1;
    }

    /**
     * 用户类型
     */
    public static final class UserType {
        public static final Integer SYSTEM = 0;
        public static final Integer ENTERPRISE = 1;
        public static final Integer AGENT = 2;
    }

    /**
     * 性别
     */
    public static final class Gender {
        public static final Integer UNKNOWN = 0;
        public static final Integer MALE = 1;
        public static final Integer FEMALE = 2;
    }

    /**
     * 是否常量
     */
    public static final class YesOrNo {
        public static final Integer NO = 0;
        public static final Integer YES = 1;
    }

    /**
     * 订单状态
     */
    public static final class OrderStatus {
        public static final Integer DRAFT = 0;        // 草稿
        public static final Integer PENDING = 1;      // 待审批
        public static final Integer APPROVED = 2;     // 已审批
        public static final Integer PROCESSING = 3;   // 处理中
        public static final Integer COMPLETED = 4;    // 已完成
        public static final Integer CANCELLED = 5;    // 已取消
        public static final Integer CLOSED = 6;       // 已关闭
    }

    /**
     * HTTP状态码
     */
    public static final class HttpStatus {
        public static final int SUCCESS = 200;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
    }
}