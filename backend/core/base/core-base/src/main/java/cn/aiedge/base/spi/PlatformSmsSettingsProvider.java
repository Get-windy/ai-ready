package cn.aiedge.base.spi;

/**
 * 平台级短信配置的读取接口（SPI）。
 *
 * <p>与 {@link PlatformMailSettingsProvider} 同源：短信配置存在 `sys_sms_config` 表，
 * 由「系统 → 平台设置 → 短信配置」（菜单 62503）维护；真正发送的是
 * `core-notification` 的 {@code SmsChannel}。依赖方向要求读取口放在 `core-base`。
 *
 * <p><b>改造前的问题</b>：{@code SmsChannel} 只读 `notification.sms.*`（application yml），
 * 而该键在本仓库 yml 中**从未声明** → {@code enabled} 恒 false → 短信渠道恒不可用。
 *
 * <p>实现方约定：**不得抛异常**，失败一律返回 {@code null}。
 */
public interface PlatformSmsSettingsProvider {

    /**
     * 取当前生效的短信配置。
     *
     * @return 配置；未配置或读取失败时返回 {@code null}
     */
    PlatformSmsSettings currentSmsSettings();

    /**
     * 平台级短信配置快照。
     *
     * @param enabled      是否启用
     * @param provider     服务商（aliyun / tencent / huawei …）
     * @param accessKey    访问密钥 ID
     * @param accessSecret 访问密钥 Secret
     * @param signName     短信签名
     */
    record PlatformSmsSettings(
            boolean enabled,
            String provider,
            String accessKey,
            String accessSecret,
            String signName
    ) {
        /** 是否具备发送的最低条件 */
        public boolean usable() {
            return enabled
                    && provider != null && !provider.isBlank()
                    && accessKey != null && !accessKey.isBlank()
                    && accessSecret != null && !accessSecret.isBlank()
                    && signName != null && !signName.isBlank();
        }
    }
}
