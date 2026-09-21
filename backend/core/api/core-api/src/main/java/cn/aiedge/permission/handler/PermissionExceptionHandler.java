package cn.aiedge.permission.handler;

import cn.aiedge.base.vo.Result;
import cn.aiedge.permission.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 权限异常处理器 —— 把 {@link PermissionDeniedException} 映射成 HTTP 403。
 *
 * <p><b>⚠️ 2026-09-21 修正（E-02 批次 4 实测发现）：</b>本类原来没有 {@code @Order}，
 * 与 core-base 的 {@code GlobalExceptionHandler}（同样没写 {@code @Order}，但有
 * {@code @ExceptionHandler(RuntimeException.class)} 兜底）优先级相同。
 * 实测抢不过兜底那个 —— 非超管打 {@code /api/user-permission/user/1/permissions}
 * 拿到的是 <b>500「系统异常，请稍后重试」</b>，而不是 403。</p>
 *
 * <p>两处修正：</p>
 * <ol>
 *   <li>声明 {@link Ordered#HIGHEST_PRECEDENCE}，让这个「专治权限拒绝」的 advice
 *       确定性地先于通用兜底命中（原来靠注册顺序，是碰运气）；</li>
 *   <li>响应体从自造的三字段 {@code {code,message,success}} 改成全局统一的
 *       {@link Result#fail(int, String)}，前端 axios 拦截器只需认一套结构。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class PermissionExceptionHandler {

    /**
     * 处理权限拒绝异常：HTTP 403 + 统一响应体
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Result<Void>> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("权限验证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }
}
