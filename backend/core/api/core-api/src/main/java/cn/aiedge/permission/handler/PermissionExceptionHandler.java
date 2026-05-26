package cn.aiedge.permission.handler;

import cn.aiedge.permission.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限异常处理器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class PermissionExceptionHandler {

    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("权限验证失败: {}", e.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.FORBIDDEN.value());
        result.put("message", e.getMessage());
        result.put("success", false);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
