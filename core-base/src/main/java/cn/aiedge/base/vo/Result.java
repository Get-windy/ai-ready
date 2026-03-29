package cn.aiedge.base.vo;

/**
 * 统一响应结果
 * 使用 Java 17 Sealed Interface
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    /**
     * 成功响应
     */
    record Success<T>(int code, String message, T data) implements Result<T> {
        public Success(T data) {
            this(200, "操作成功", data);
        }

        public Success(String message, T data) {
            this(200, message, data);
        }
    }

    /**
     * 失败响应
     */
    record Failure<T>(int code, String message, T data) implements Result<T> {
        public Failure(String message) {
            this(500, message, null);
        }

        public Failure(int code, String message) {
            this(code, message, null);
        }
    }

    /**
     * 快捷成功方法
     */
    static <T> Result<T> ok(T data) {
        return new Success<>(data);
    }

    /**
     * 快捷成功方法
     */
    static <T> Result<T> ok(String message, T data) {
        return new Success<>(message, data);
    }

    /**
     * 快捷失败方法
     */
    static <T> Result<T> fail(String message) {
        return new Failure<>(message);
    }

    /**
     * 快捷失败方法
     */
    static <T> Result<T> fail(int code, String message) {
        return new Failure<>(code, message);
    }
}