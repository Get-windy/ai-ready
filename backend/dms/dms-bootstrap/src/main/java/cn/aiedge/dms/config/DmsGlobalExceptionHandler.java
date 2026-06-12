package cn.aiedge.dms.config;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.stream.Collectors;

/**
 * DMS 全局异常处理器
 *
 * 统一返回 ApiResponse 格式，确保与 DMS 控制器返回结构一致。
 * 覆盖 core-base 的 GlobalExceptionHandler，仅处理 cn.aiedge.dms 包下的异常。
 *
 * @author AI-Ready Team
 */
@Slf4j
@RestControllerAdvice(basePackages = "cn.aiedge.dms")
public class DmsGlobalExceptionHandler {

    @Autowired
    private HttpServletRequest request;

    /**
     * Sa-Token 未登录
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问: uri={}", request.getRequestURI());
        return ApiResponse.unauthorized("请先登录");
    }

    /**
     * Sa-Token 无权限
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleNoPermission(NotPermissionException e) {
        log.warn("无权限访问: uri={}, permission={}", request.getRequestURI(), e.getPermission());
        return ApiResponse.forbidden("无权限访问");
    }

    /**
     * Sa-Token 无角色
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleNoRole(NotRoleException e) {
        log.warn("无角色权限: uri={}, role={}", request.getRequestURI(), e.getRole());
        return ApiResponse.forbidden("无角色权限");
    }

    /**
     * JSR-303 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: uri={}, message={}", request.getRequestURI(), message);
        return ApiResponse.badRequest(message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBind(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.badRequest(message);
    }

    /**
     * 请求体解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: uri={}", request.getRequestURI());
        return ApiResponse.badRequest("请求参数格式错误");
    }

    /**
     * 参数类型转换失败
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败: name={}, value={}", e.getName(), e.getValue());
        return ApiResponse.badRequest("参数[" + e.getName() + "]格式不正确");
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.badRequest("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 数据完整性违例（唯一约束冲突等）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据完整性违例: uri={}, detail={}", request.getRequestURI(),
                e.getMostSpecificCause().getMessage());
        return ApiResponse.badRequest("请求数据不完整或存在冲突");
    }

    /**
     * 业务异常（含 DmsBusinessException）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        if (e.getCode() >= 500) {
            log.error("业务异常: uri={}, code={}, message={}",
                    request.getRequestURI(), e.getCode(), e.getMessage(), e);
        } else {
            log.warn("业务异常: uri={}, code={}, message={}",
                    request.getRequestURI(), e.getCode(), e.getMessage());
        }
        HttpStatus httpStatus = HttpStatus.resolve(e.getCode());
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(ApiResponse.fail(e.getCode(), e.getMessage()), httpStatus);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return ApiResponse.badRequest(e.getMessage());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常: uri={}, method={}", request.getRequestURI(), request.getMethod(), e);
        return ApiResponse.fail(500, "系统异常，请稍后重试");
    }
}
