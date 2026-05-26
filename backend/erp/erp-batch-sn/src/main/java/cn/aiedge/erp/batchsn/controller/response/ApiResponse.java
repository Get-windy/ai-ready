package cn.aiedge.erp.batchsn.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * 统一的API响应格式
 *
 * @author team-member
 * @date 2026-05-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP状态码
     */
    private int status;

    /**
     * 业务状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 分页信息
     */
    private Pagination pagination;

    /**
     * 创建成功响应（有数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .build();
    }

    /**
     * 创建成功响应（有分页数据）
     */
    public static <T> ApiResponse<T> success(T data, Pagination pagination) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .pagination(pagination)
                .build();
    }

    /**
     * 创建创建成功响应（201）
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .code("CREATED")
                .message("资源创建成功")
                .data(data)
                .build();
    }

    /**
     * 创建错误响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建错误响应（指定HTTP状态码）
     */
    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建未找到响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .code("NOT_FOUND")
                .message(message)
                .build();
    }

    /**
     * 创建未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .code("UNAUTHORIZED")
                .message(message)
                .build();
    }

    /**
     * 创建禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("FORBIDDEN")
                .message(message)
                .build();
    }

    /**
     * 创建服务错误响应
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_ERROR")
                .message(message)
                .build();
    }

    /**
     * 分页信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Pagination {
        /**
         * 当前页码（从1开始）
         */
        private int page;

        /**
         * 每页记录数
         */
        private int size;

        /**
         * 总记录数
         */
        private long total;

        /**
         * 总页数
         */
        private int totalPages;

        /**
         * 是否有上一页
         */
        private boolean hasPrevious;

        /**
         * 是否有下一页
         */
        private boolean hasNext;

        /**
         * 从分页参数创建分页信息
         */
        public static Pagination of(int page, int size, long total) {
            int totalPages = (int) Math.ceil((double) total / size);
            
            return Pagination.builder()
                    .page(page)
                    .size(size)
                    .total(total)
                    .totalPages(totalPages)
                    .hasPrevious(page > 1)
                    .hasNext(page < totalPages)
                    .build();
        }
    }
}