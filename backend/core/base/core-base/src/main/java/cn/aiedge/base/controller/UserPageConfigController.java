package cn.aiedge.base.controller;

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
import java.util.Map;

/**
 * 用户页面配置控制器
 * <p>
 * 存储用户个人页面配置（列显隐、查询条件、功能按钮等），
 * 使用 {@code user_page_config:{module}:{page}:{userId}} 作为配置键，
 * 复用 sys_project_config 表存储。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/system/user-config")
@RequiredArgsConstructor
@Tag(name = "用户页面配置")
public class UserPageConfigController {

    private final SysProjectConfigMapper configMapper;

    @GetMapping("/{module}/{page}")
    @SaCheckLogin
    @Operation(summary = "获取用户页面配置")
    public Result<String> getPageConfig(@PathVariable String module, @PathVariable String page) {
        long userId = StpUtil.getLoginIdAsLong();
        String key = buildKey(module, page, userId);
        SysProjectConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysProjectConfig>()
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
                .eq(SysProjectConfig::getConfigKey, key)
                .eq(SysProjectConfig::getDeleted, 0);
        SysProjectConfig config = configMapper.selectOne(wrapper);

        if (config != null) {
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        } else {
            config = new SysProjectConfig();
            config.setTenantId(1L);
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
}
