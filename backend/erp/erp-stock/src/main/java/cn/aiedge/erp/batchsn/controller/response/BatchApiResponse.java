package cn.aiedge.erp.batchsn.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchApiResponse<T> {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;

    private String code;

    private String message;

    private T data;

    private Pagination pagination;

    public static <T> BatchApiResponse<T> success(T data) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }

    public static <T> BatchApiResponse<T> success() {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .build();
    }

    public static <T> BatchApiResponse<T> success(T data, Pagination pagination) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .pagination(pagination)
                .build();
    }

    public static <T> BatchApiResponse<T> created(T data) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .code("CREATED")
                .message("资源创建成功")
                .data(data)
                .build();
    }

    public static <T> BatchApiResponse<T> error(String message) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("ERROR")
                .message(message)
                .build();
    }

    public static <T> BatchApiResponse<T> error(int status, String code, String message) {
        return BatchApiResponse.<T>builder()
                .status(status)
                .code(code)
                .message(message)
                .build();
    }

    public static <T> BatchApiResponse<T> notFound(String message) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .code("NOT_FOUND")
                .message(message)
                .build();
    }

    public static <T> BatchApiResponse<T> unauthorized(String message) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .code("UNAUTHORIZED")
                .message(message)
                .build();
    }

    public static <T> BatchApiResponse<T> forbidden(String message) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("FORBIDDEN")
                .message(message)
                .build();
    }

    public static <T> BatchApiResponse<T> serverError(String message) {
        return BatchApiResponse.<T>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_ERROR")
                .message(message)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Pagination {
        private int page;
        private int size;
        private long total;
        private int totalPages;
        private boolean hasPrevious;
        private boolean hasNext;

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