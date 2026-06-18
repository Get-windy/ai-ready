package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.dto.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 地图服务抽象基类
 * 提供公共方法，子类只需实现特定地图服务的 API 调用逻辑。
 */
@Slf4j
public abstract class AbstractMapService implements MapService {

    protected final String apiKey;

    protected AbstractMapService(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 执行 HTTP GET 请求并返回字符串响应
     */
    protected abstract String doGet(String url);

    /**
     * 安全的地理编码（异常时返回空结果，不影响主流程）
     */
    protected GeocodeResponse safeGeocode(GeocodeRequest request) {
        try {
            return geocode(request);
        } catch (Exception e) {
            log.warn("[{}] 地理编码失败: address={}, error={}", getProvider(), request.getAddress(), e.getMessage());
            return GeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }

    /**
     * 安全的逆地理编码
     */
    protected ReverseGeocodeResponse safeReverseGeocode(ReverseGeocodeRequest request) {
        try {
            return reverseGeocode(request);
        } catch (Exception e) {
            log.warn("[{}] 逆地理编码失败: lat={},lng={}, error={}",
                    getProvider(), request.getLat(), request.getLng(), e.getMessage());
            return ReverseGeocodeResponse.builder().success(false).message(e.getMessage()).build();
        }
    }
}
