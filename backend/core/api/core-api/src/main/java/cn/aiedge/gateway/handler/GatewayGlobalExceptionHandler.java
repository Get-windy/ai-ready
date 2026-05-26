package cn.aiedge.gateway.handler;

import cn.aiedge.base.vo.Result;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 网关全局异常处理器
 * 统一处理网关层的各种异常
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class GatewayGlobalExceptionHandler extends DefaultErrorWebExceptionHandler {

    public GatewayGlobalExceptionHandler(ErrorAttributes errorAttributes, WebProperties.Resources resources,
                                       ErrorProperties errorProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resources, errorProperties, applicationContext);
    }

    /**
     * 获取异常属性
     */
    @Override
    protected Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        int code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        Throwable error = getError(request);
        String message = "网关内部错误";

        // 根据不同异常类型返回不同的错误信息
        if (error instanceof NotFoundException) {
            message = "服务未找到";
            code = HttpStatus.NOT_FOUND.value();
        } else if (error instanceof TimeoutException) {
            message = "服务调用超时";
            code = HttpStatus.REQUEST_TIMEOUT.value();
        } else if (error instanceof org.springframework.web.client.ResourceAccessException) {
            message = "服务暂时不可用";
            code = HttpStatus.SERVICE_UNAVAILABLE.value();
        }

        return Result.error(code, error.getMessage() != null ? error.getMessage() : message).toMap();
    }

    /**
     * 获取路由函数
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 渲染错误响应
     */
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        return ServerResponse.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorAttributes));
    }
}
