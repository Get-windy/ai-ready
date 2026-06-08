package cn.aiedge.base.config;

import cn.aiedge.base.log.ErrorLogService;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.exception.BusinessException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
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
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired(required = false)
    private ErrorLogService errorLogService;

    @Autowired
    private HttpServletRequest request;

    /**
     * 处理 Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        return Result.fail(401, "请先登录");
    }

    /**
     * 处理 Sa-Token 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("无权限异常: {}", e.getPermission());
        return Result.fail(403, "无权限访问: " + e.getPermission());
    }

    /**
     * 处理 Sa-Token 无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("无角色异常: {}", e.getRole());
        return Result.fail(403, "无角色权限: " + e.getRole());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /**
     * 处理请求体缺失或格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        // 4xx 客户端错误不写入错误日志文件，避免日志污染
        return Result.fail(400, "请求参数格式错误");
    }

    /**
     * 处理方法参数类型转换失败（如字符串传入了数值字段）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败: name={}, value={}, requiredType={}",
            e.getName(), e.getValue(), e.getRequiredType());
        // 4xx 客户端错误不写入错误日志文件，避免日志污染
        return Result.fail(400, "参数[" + e.getName() + "]格式不正确");
    }

    /**
     * 处理类型不匹配异常（如路径变量传入了字符串）
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatchException(TypeMismatchException e) {
        log.warn("类型不匹配: {}", e.getMessage());
        // 4xx 客户端错误不写入错误日志文件，避免日志污染
        return Result.fail(400, "请求参数格式不正确");
    }

    /**
     * 处理缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理404资源未找到
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源未找到: {} {}", e.getHttpMethod(), e.getResourcePath());
        return Result.fail(404, "接口不存在: " + e.getResourcePath());
    }

    /**
     * 处理请求方法不支持（如 POST 到 GET 端点）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {} -> {}", e.getMethod(), String.join(",", e.getSupportedMethods()));
        return Result.fail(405, "请求方法不支持，请使用 " + String.join(",", e.getSupportedMethods()));
    }

    /**
     * 处理不支持的 Content-Type（如 text/plain 提交到 JSON 接口）
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的 Content-Type: {}", e.getContentType());
        return Result.fail(415, "不支持的 Content-Type: " + e.getContentType());
    }

    /**
     * 处理数据完整性违例（如 NOT NULL 约束、唯一约束冲突）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("数据完整性违例: {}", e.getMostSpecificCause().getMessage());
        // 4xx 客户端错误不写入错误日志文件，避免日志污染
        return Result.fail(400, "请求数据不完整或存在冲突");
    }

    /**
     * 处理不接受的 Accept 类型
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public Result<Void> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn("不接受的 Accept 类型: {}", e.getMessage());
        return Result.fail(406, "不支持的响应格式");
    }

    /**
     * 处理业务异常（带错误码）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        if (e.getCode() >= 500) {
            log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage(), e);
            logToErrorService(e, "BusinessException");
        } else {
            log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        }
        // 根据错误码映射HTTP状态码
        HttpStatus httpStatus = HttpStatus.resolve(e.getCode());
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(Result.fail(e.getCode(), e.getMessage()), httpStatus);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: ", e);
        logToErrorService(e, "RuntimeException");
        return Result.fail(500, "系统异常，请稍后重试");
    }

    /**
     * 处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        logToErrorService(e, "Exception");
        return Result.fail(500, "系统异常，请稍后重试");
    }

    /**
     * 将异常记录到错误日志服务
     */
    private void logToErrorService(Throwable e, String source) {
        if (errorLogService != null) {
            Map<String, Object> context = new HashMap<>();
            context.put("requestUri", request.getRequestURI());
            context.put("requestMethod", request.getMethod());
            context.put("remoteAddr", request.getRemoteAddr());
            context.put("queryString", request.getQueryString());
            errorLogService.logException(e, source, context);
        }
    }
}