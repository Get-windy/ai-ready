package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import cn.aiedge.base.service.MenuVisibilityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 租户级「菜单显隐」服务实现：读写 {@code sys_project_config} 的隐藏菜单集合。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuVisibilityServiceImpl implements MenuVisibilityService {

    private final SysProjectConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Set<Long> getHiddenMenuIds(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptySet();
        }
        // getConfigValue 的 SQL 已带 tenant_id + deleted = 0 + status = 0
        String value = configMapper.getConfigValue(tenantId, CONFIG_KEY);
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        try {
            List<Long> ids = objectMapper.readValue(value, new TypeReference<List<Long>>() {});
            return ids == null ? Collections.emptySet() : new LinkedHashSet<>(ids);
        } catch (Exception e) {
            // 配置损坏时按「全部显示」兜底，避免一个坏值让整棵菜单树消失
            log.warn("[菜单配置] 隐藏菜单配置解析失败，按全部显示处理: tenantId={}, value={}", tenantId, value, e);
            return Collections.emptySet();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisibility(Long tenantId, Long menuId, boolean visible) {
        if (tenantId == null || menuId == null) {
            return;
        }
        Set<Long> hidden = new TreeSet<>(getHiddenMenuIds(tenantId));
        boolean changed = visible ? hidden.remove(menuId) : hidden.add(menuId);
        if (!changed) {
            // 幂等：目标状态与当前一致时不写库
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(hidden);
        } catch (Exception e) {
            throw new RuntimeException("菜单显隐配置序列化失败", e);
        }

        // 与 UserPageConfigController 同口径：sys_project_config 没有 (tenant_id, config_key) 唯一索引，
        // 并发写入可能产生重复行，这里先清理多余行，避免后续按 key 取配置时出现多行歧义
        List<SysProjectConfig> rows = configMapper.selectList(new LambdaQueryWrapper<SysProjectConfig>()
                .eq(SysProjectConfig::getTenantId, tenantId)
                .eq(SysProjectConfig::getConfigKey, CONFIG_KEY)
                .orderByAsc(SysProjectConfig::getId));
        if (rows.size() > 1) {
            for (int i = 1; i < rows.size(); i++) {
                configMapper.deleteById(rows.get(i).getId());
            }
        }

        if (!rows.isEmpty()) {
            SysProjectConfig config = rows.get(0);
            config.setConfigValue(json);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            SysProjectConfig config = new SysProjectConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(CONFIG_KEY);
            config.setConfigValue(json);
            config.setConfigType("json");
            config.setConfigGroup(CONFIG_GROUP);
            config.setDescription("菜单配置：本租户隐藏的菜单 id 列表（JSON 数组）");
            config.setStatus(0);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        }
        log.info("[菜单配置] 租户级菜单显隐已更新: tenantId={}, menuId={}, visible={}", tenantId, menuId, visible);
    }
}
