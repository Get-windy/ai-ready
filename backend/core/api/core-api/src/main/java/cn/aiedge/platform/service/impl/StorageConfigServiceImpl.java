package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.mapper.StorageConfigMapper;
import cn.aiedge.platform.model.StorageConfig;
import cn.aiedge.platform.service.StorageConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageConfigServiceImpl implements StorageConfigService {

    private final StorageConfigMapper storageConfigMapper;

    @Override
    public StorageConfig getConfig(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        StorageConfig config = storageConfigMapper.selectOne(
                new LambdaQueryWrapper<StorageConfig>()
                        .eq(StorageConfig::getTenantId, tenantId)
        );
        if (config == null) {
            config = storageConfigMapper.selectOne(
                    new LambdaQueryWrapper<StorageConfig>().last("LIMIT 1")
            );
        }
        return config;
    }

    @Override
    public StorageConfig saveConfig(StorageConfig config, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        StorageConfig existing = storageConfigMapper.selectOne(
                new LambdaQueryWrapper<StorageConfig>()
                        .eq(StorageConfig::getTenantId, tenantId)
        );
        config.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            config.setCreateTime(now);
            config.setUpdateTime(now);
            storageConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            config.setCreateTime(existing.getCreateTime());
            config.setUpdateTime(now);
            storageConfigMapper.updateById(config);
        }
        log.info("保存存储配置: tenantId={}, type={}", tenantId, config.getStorageType());
        return config;
    }

    /**
     * 存储连通性测试。
     *
     * <p>改造前此方法只打日志然后 {@code return true}。
     *
     * <p>按存储类型分流：
     * <ul>
     *   <li><b>本地存储（local）</b> —— 真做一次**写入探测**：目录不存在则尝试创建，
     *       然后写入一个随机命名的小文件再删除。只有真写得进去才算通过
     *       （仅判断"目录存在"是不够的：目录存在但只读、或磁盘满，都会在真正上传时才暴露）。</li>
     *   <li><b>对象存储（oss/s3/minio/cos/obs…）</b> —— 解析 {@code endpoint} 做 TCP 建连探测。
     *       本系统**未集成对象存储 SDK**，故无法验证 AccessKey/Secret 与 bucket 是否存在，
     *       返回文案会明确说明这一点。</li>
     * </ul>
     * <b>安全</b>：探测文件固定落在配置的 {@code localPath} 下，文件名服务端生成，
     * 写完即删，不留残留。
     */
    @Override
    public ConnectionTestResult testConnection(StorageConfig config) {
        if (config == null) {
            return ConnectionTestResult.fail("没有可测试的配置");
        }
        String type = trimToNull(config.getStorageType());
        if (type == null) {
            return ConnectionTestResult.fail("请先选择存储类型（storageType）");
        }

        if ("local".equalsIgnoreCase(type)) {
            return testLocal(config);
        }
        return testRemote(config, type);
    }

    private ConnectionTestResult testLocal(StorageConfig config) {
        String localPath = trimToNull(config.getLocalPath());
        if (localPath == null) {
            return ConnectionTestResult.fail("请先填写本地存储目录（localPath）");
        }
        Path dir;
        try {
            dir = Path.of(localPath).toAbsolutePath().normalize();
        } catch (Exception e) {
            return ConnectionTestResult.fail("本地存储目录路径非法：" + localPath);
        }

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            if (!Files.isDirectory(dir)) {
                return ConnectionTestResult.fail("本地存储目录不是一个目录：" + dir);
            }
        } catch (IOException e) {
            return ConnectionTestResult.fail("本地存储目录不可用（创建失败）：" + dir + " —— " + e.getMessage());
        }

        // 真写一个探测文件：只判断 isWritable 会漏掉"磁盘满"这类只在写入时暴露的问题
        Path probe = dir.resolve(".write-probe-" + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(probe, "probe", StandardCharsets.UTF_8);
            Files.deleteIfExists(probe);
            log.info("本地存储写入探测通过: {}", dir);
            return ConnectionTestResult.ok("本地存储可用（已通过写入探测）：" + dir);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(probe);
            } catch (IOException ignored) {
                // 探测文件本身写失败时删不掉是正常的，忽略
            }
            log.warn("本地存储写入探测失败: {}", dir, e);
            return ConnectionTestResult.fail("本地存储目录不可写：" + dir + " —— " + e.getMessage());
        }
    }

    private ConnectionTestResult testRemote(StorageConfig config, String type) {
        String endpoint = trimToNull(config.getEndpoint());
        if (endpoint == null) {
            return ConnectionTestResult.fail("请先填写对象存储 Endpoint");
        }
        if (trimToNull(config.getBucket()) == null) {
            return ConnectionTestResult.fail("请先填写 Bucket 名称");
        }

        String host;
        int port;
        try {
            URI uri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
            host = uri.getHost();
            if (host == null) {
                return ConnectionTestResult.fail("Endpoint 格式不合法：" + endpoint);
            }
            port = uri.getPort() > 0 ? uri.getPort()
                    : ("http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443);
        } catch (Exception e) {
            return ConnectionTestResult.fail("Endpoint 格式不合法：" + endpoint);
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            log.info("对象存储端点连通: type={}, host={}, port={}", type, host, port);
            return ConnectionTestResult.ok(
                    "对象存储端点连通（" + host + ":" + port + "，bucket=" + config.getBucket() + "）。"
                            + "注意：仅验证了网络连通，未校验 AccessKey/Secret 与 bucket 是否真实存在"
                            + "（本系统未集成对象存储 SDK）。");
        } catch (Exception e) {
            log.warn("对象存储端点不可达: host={}, port={}", host, port, e);
            return ConnectionTestResult.fail(
                    "对象存储端点不可达（" + host + ":" + port + "）：" + e.getClass().getSimpleName()
                            + (e.getMessage() == null ? "" : " - " + e.getMessage()));
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
