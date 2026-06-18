package cn.aiedge.dms.config.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.mapper.DmsConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DMS 配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final DmsConfigMapper dmsConfigMapper;

    /**
     * 获取租户配置
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return DmsConfig
     */
    public DmsConfig getConfig(Long tenantId, String key) {
        DmsConfig config = dmsConfigMapper.selectOne(
                new LambdaQueryWrapper<DmsConfig>()
                        .eq(DmsConfig::getTenantId, tenantId)
                        .eq(DmsConfig::getConfigKey, key)
        );
        if (config == null) {
            throw new DmsBusinessException("配置不存在: key=" + key + ", tenantId=" + tenantId);
        }
        return config;
    }

    /**
     * 更新租户配置
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @param value    配置值
     * @return DmsConfig
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsConfig updateConfig(Long tenantId, String key, String value) {
        DmsConfig config = getConfig(tenantId, key);
        config.setConfigValue(value);
        dmsConfigMapper.updateById(config);
        log.info("配置已更新: key={}, tenantId={}, value={}", key, tenantId, value);
        return config;
    }

    /**
     * 获取租户所有配置
     *
     * @param tenantId 租户ID
     * @return 配置列表
     */
    public List<DmsConfig> listAll(Long tenantId) {
        return dmsConfigMapper.selectList(
                new LambdaQueryWrapper<DmsConfig>()
                        .eq(DmsConfig::getTenantId, tenantId)
                        .orderByAsc(DmsConfig::getConfigKey)
        );
    }

    /**
     * 获取字符串类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 字符串值
     */
    public String getString(Long tenantId, String key) {
        return getConfig(tenantId, key).getConfigValue();
    }

    /**
     * 获取整数类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 整数值
     */
    public Integer getInteger(Long tenantId, String key) {
        String value = getString(tenantId, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("配置值不是有效的整数: key=" + key + ", value=" + value);
        }
    }

    /**
     * 获取布尔类型配置值
     *
     * @param tenantId 租户ID
     * @param key      配置键
     * @return 布尔值
     */
    public Boolean getBoolean(Long tenantId, String key) {
        String value = getString(tenantId, key);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
