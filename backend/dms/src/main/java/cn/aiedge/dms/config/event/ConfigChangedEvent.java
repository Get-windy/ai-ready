package cn.aiedge.dms.config.event;

/**
 * DMS 配置变更事件（进程内）
 *
 * 配置保存后由 {@code ConfigService} 发布，订阅方（如地图 Key 解析器）据此**立即失效本地缓存**，
 * 无需重启即可生效；多实例部署时其它实例最迟在缓存 TTL 内自动刷新。
 *
 * @param configKey 变更的配置键
 * @param value     新值
 */
public record ConfigChangedEvent(String configKey, String value) {
}
