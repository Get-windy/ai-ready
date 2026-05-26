package cn.aiedge.gateway.fallback;

import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

/**
 * 网关熔断降级处理
 * 当下游服务不可用时提供降级响应
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
public class GatewayFallbackController {

    /**
     * 服务不可用降级处理
     * 
     * @return 降级响应
     */
    @RequestMapping("/fallback/service-unavailable")
    public Mono<ServerResponse> serviceUnavailable() {
        return ServerResponse
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{\"code\": 503, \"message\": \"服务暂时不可用，请稍后重试\", \"timestamp\": " + System.currentTimeMillis() + "}"));
    }

    /**
     * 超时降级处理
     * 
     * @return 降级响应
     */
    @RequestMapping("/fallback/timeout")
    public Mono<ServerResponse> timeout() {
        return ServerResponse
            .status(HttpStatus.REQUEST_TIMEOUT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("{\"code\": 408, \"message\": \"请求超时，请稍后重试\", \"timestamp\": " + System.currentTimeMillis() + "}"));
    }
}
