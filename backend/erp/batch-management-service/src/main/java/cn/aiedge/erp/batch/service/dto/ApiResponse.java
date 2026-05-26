package cn.aiedge.erp.batch.service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通用API响应DTO
 * 用于统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 请求ID（用于追踪）
     */
    private String requestId;
    
    /**
     * 成功响应工厂方法
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "操作成功");
    }
    
    /**
     * 成功响应工厂方法
     *
     * @param data 响应数据
     * @param message 响应消息
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 失败响应工厂方法
     *
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 失败响应工厂方法（带数据）
     *
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误数据
     * @param <T> 数据类型
     * @return API响应对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 判断响应是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code != null && code >= 200 && code < 300;
    }
    
    /**
     * 构建分页响应数据
     *
     * @param data 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     * @return 分页数据对象
     */
    public static <T> Object buildPageData(T data, long total, int page, int size) {
        return new PageData<>(data, total, page, size);
    }
    
    /**
     * 分页数据内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class PageData<T> {
        /**
         * 数据列表
         */
        private T data;
        
        /**
         * 总记录数
         */
        private long total;
        
        /**
         * 当前页码
         */
        private int page;
        
        /**
         * 每页大小
         */
        private int size;
        
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
        
        public PageData(T data, long total, int page, int size) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.size = size;
            this.totalPages = (int) Math.ceil((double) total / size);
            this.hasPrevious = page > 1;
            this.hasNext = page < totalPages;
        }
    }
}