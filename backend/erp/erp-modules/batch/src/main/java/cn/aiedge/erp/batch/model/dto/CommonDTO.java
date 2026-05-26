package cn.aiedge.erp.batch.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用DTO类
 */
public class CommonDTO {
    
    /**
     * 分页查询响应DTO
     */
    @Data
    public static class PageResult<T> {
        private List<T> content;
        private long totalElements;
        private int totalPages;
        private int pageNumber;
        private int pageSize;
        private boolean first;
        private boolean last;
        private boolean empty;
    }
    
    /**
     * 通用响应DTO
     */
    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private String code;
        private String message;
        private T data;
        private Long timestamp;
        
        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            response.setMessage("操作成功");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> success(String message, T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            response.setMessage(message);
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String code, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setCode(code);
            response.setMessage(message);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            return error("ERROR", message);
        }
    }
    
    /**
     * ID请求DTO
     */
    @Data
    public static class IdRequest {
        private Long id;
    }
    
    /**
     * 批量ID请求DTO
     */
    @Data
    public static class BatchIdRequest {
        private List<Long> ids;
    }
    
    /**
     * 分页查询请求DTO
     */
    @Data
    public static class PageRequest {
        private int pageNumber = 0;
        private int pageSize = 10;
        private String sortField;
        private String sortDirection = "DESC";
    }
    
    /**
     * 通用搜索条件DTO
     */
    @Data
    public static class SearchCriteria {
        private String keyword;
        private List<Long> ids;
        private String status;
        private String type;
        private String startDate;
        private String endDate;
    }
}