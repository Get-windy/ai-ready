package cn.aiedge.base.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户页面配置控制器
 * <p>
 * 存储用户个人页面配置（列显隐、查询条件、功能按钮等），
 * 使用 {@code user_page_config:{module}:{page}:{userId}} 作为配置键，
 * 复用 sys_project_config 表存储。
 * </p>
 *
 * <p><b>2026-09-21 租户专项</b>：这里原先写的是 {@code config.setTenantId(1L)} —— 与
 * {@code SysConfigServiceImpl} 里那句 {@code CURRENT_TENANT_ID = 1L} 是同一类硬编码，
 * 即「所有租户的用户页面配置都落在租户 1 名下」。它当时没暴露出串数据，只是因为
 * 配置键末尾拼了全局唯一的 userId；一旦哪天键里的用户标识改成租户内序号就会立刻串。
 * 现在：写入落**会话租户**（解析不到时落平台行 0），读取/更新也带租户条件。</p>
 *
 * <p>⚠️ 存量数据已核对（真库 15 行全部属于租户 1 的用户），加租户条件不会让任何人的配置"消失"。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/system/user-config")
@RequiredArgsConstructor
@Tag(name = "用户页面配置")
public class UserPageConfigController {

    /** 平台默认行的租户 ID（与 SysConfigServiceImpl.PLATFORM_TENANT_ID 同一约定） */
    private static final Long PLATFORM_TENANT_ID = 0L;

    private final SysProjectConfigMapper configMapper;

    @GetMapping("/{module}/{page}")
    @SaCheckLogin
    @Operation(summary = "获取用户页面配置")
    public Result<String> getPageConfig(@PathVariable String module, @PathVariable String page) {
        long userId = StpUtil.getLoginIdAsLong();
        String key = buildKey(module, page, userId);
        SysProjectConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysProjectConfig>()
                        .eq(SysProjectConfig::getTenantId, scopedTenantId())
                        .eq(SysProjectConfig::getConfigKey, key)
                        .eq(SysProjectConfig::getDeleted, 0)
                        .last("LIMIT 1")
        );
        return Result.ok(config != null ? config.getConfigValue() : "");
    }

    @PostMapping("/{module}/{page}")
    @SaCheckLogin
    @Operation(summary = "保存用户页面配置")
    public Result<Void> savePageConfig(
            @PathVariable String module,
            @PathVariable String page,
            @RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        String value = body.get("value");
        if (value == null) {
            return Result.fail("value 不能为空");
        }

        String key = buildKey(module, page, userId);
        LambdaQueryWrapper<SysProjectConfig> wrapper = new LambdaQueryWrapper<SysProjectConfig>()
                .eq(SysProjectConfig::getTenantId, scopedTenantId())
                .eq(SysProjectConfig::getConfigKey, key)
                .eq(SysProjectConfig::getDeleted, 0)
                .orderByAsc(SysProjectConfig::getId);
        List<SysProjectConfig> list = configMapper.selectList(wrapper);
        // 并发写入可能产生重复记录，此处清理多余记录避免 selectOne 抛 TooManyResultsException
        if (list.size() > 1) {
            for (int i = 1; i < list.size(); i++) {
                configMapper.deleteById(list.get(i).getId());
            }
        }

        SysProjectConfig config;
        if (!list.isEmpty()) {
            config = list.get(0);
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            config = new SysProjectConfig();
            // 会话租户（**不是**常量 1）；MetaObjectHandler 只在 tenantId 为 null 时兜底，
            // 这里显式写死来源，避免"看起来是自动填的、实际是别人填的"
            config.setTenantId(scopedTenantId());
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("json");
            config.setConfigGroup("user_page_config");
            config.setStatus(0);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            configMapper.insert(config);
        }

        return Result.ok();
    }

    private static String buildKey(String module, String page, long userId) {
        return "user_page_config:" + module + ":" + page + ":" + userId;
    }

    /**
     * 配置归属租户 = 当前会话租户；解析不到时用平台行（0）。
     *
     * <p>本控制器全部端点都是 {@code @SaCheckLogin}，正常不会走到兜底分支；
     * 兜底取 0 而不是 1，是因为「猜一个具体租户」正是本专项要根除的写法。</p>
     */
    private static Long scopedTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return tenantId != null ? tenantId : PLATFORM_TENANT_ID;
    }
}
