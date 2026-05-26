package cn.aiedge.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应对象
 */
@Data
public class R<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String msg;
    private T data;
    private Boolean success;
    
    public static <T> R<T> ok() {
        return ok(null);
    }
    
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        r.setSuccess(true);
        return r;
    }
    
    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMsg(msg);
        r.setSuccess(false);
        return r;
    }
    
    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setSuccess(false);
        return r;
    }
    
    public Boolean isSuccess() {
        return success;
    }
}