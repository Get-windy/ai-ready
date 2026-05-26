package com.aiready.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回结果类
 */
@Data
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String message;
    private T data;
    
    public Result() {
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    public static <T> Result<T> failed() {
        return new Result<>(500, "failed", null);
    }
    
    public static <T> Result<T> failed(String message) {
        return new Result<>(500, message, null);
    }
    
    public static <T> Result<T> failed(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> failed(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
    
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    public boolean isFailed() {
        return !isSuccess();
    }
}