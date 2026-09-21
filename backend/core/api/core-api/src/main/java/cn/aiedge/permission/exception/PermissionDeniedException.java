package cn.aiedge.permission.exception;

import cn.aiedge.common.exception.BusinessException;

/**
 * 权限拒绝异常 —— {@code @RequirePermission} / {@code @RequireRole} 不通过时由
 * {@code cn.aiedge.permission.aspect.PermissionAspect} 抛出。
 *
 * <p><b>⚠️ 2026-09-21 修正（E-02 批次 4 实测发现）：</b>本类原来直接
 * {@code extends RuntimeException}，而 core-base 的 {@code GlobalExceptionHandler} 里有一个
 * {@code @ExceptionHandler(RuntimeException.class)} 兜底。两个 {@code @RestControllerAdvice}
 * 都没写 {@code @Order}，兜底那个先命中，于是<b>权限拒绝被吞成 HTTP 500
 * 「系统异常，请稍后重试」</b>：调用方无法区分「你没有这个权限（403）」和「服务崩了（500）」，
 * 客户端会把权限不足错报成系统故障，两向验证里「非超管必须 403」这条断言也会被误判。</p>
 *
 * <p>现在继承 {@link BusinessException}（code=403）作为双保险：</p>
 * <ol>
 *   <li>本模块的 {@code PermissionExceptionHandler} 精确匹配它（且已声明最高优先级）；</li>
 *   <li>万一仍被 core-base 的兜底 advice 先命中，也会走 {@code handleBusinessException}
 *       按 code 映射成 HTTP 403，而不是 500。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class PermissionDeniedException extends BusinessException {

    /** 权限拒绝固定 403，不允许调用方改码（否则又会与系统异常混淆）。 */
    private static final int FORBIDDEN = 403;

    public PermissionDeniedException(String message) {
        super(FORBIDDEN, message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(FORBIDDEN, message, cause);
    }
}
