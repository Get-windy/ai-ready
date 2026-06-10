package cn.aiedge.user.controller;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * 个人中心控制器
 */
@Slf4j
@Tag(name = "个人中心", description = "当前用户个人信息、密码、偏好设置等接口")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "获取个人信息")
    @GetMapping
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        // 获取角色列表
        List<String> roleCodes = sysUserService.getRoleCodes(userId);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname() != null ? user.getNickname() : "");
        profile.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        profile.put("email", user.getEmail() != null ? user.getEmail() : "");
        profile.put("phone", user.getPhone() != null ? user.getPhone() : "");
        profile.put("gender", user.getGender() != null ? user.getGender() : 0);
        profile.put("roleNames", roleCodes != null ? roleCodes : Collections.emptyList());
        profile.put("deptName", "");

        return ApiResponse.ok(profile);
    }

    @Operation(summary = "更新个人信息")
    @PutMapping
    @SaCheckLogin
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        if (body.containsKey("nickname")) user.setNickname((String) body.get("nickname"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("gender")) user.setGender((Integer) body.get("gender"));
        if (body.containsKey("avatar")) user.setAvatar((String) body.get("avatar"));

        sysUserService.updateUser(user);
        return ApiResponse.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @SaCheckLogin
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (oldPassword == null || newPassword == null || confirmPassword == null) {
            return ApiResponse.fail("参数不完整");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ApiResponse.fail("两次输入的密码不一致");
        }
        if (newPassword.length() < 6) {
            return ApiResponse.fail("密码长度不能少于6位");
        }

        sysUserService.changePassword(userId, oldPassword, newPassword);
        return ApiResponse.success();
    }

    @Operation(summary = "获取偏好设置")
    @GetMapping("/preferences")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> getPreferences() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        Map<String, Object> prefs = parseExtInfo(user.getExtInfo());
        if (prefs == null) {
            prefs = new HashMap<>();
        }

        // 合并默认值，确保所有字段都存在
        Map<String, Object> defaults = getDefaultPreferences();
        defaults.putAll(prefs);

        return ApiResponse.ok(defaults);
    }

    @Operation(summary = "更新偏好设置")
    @PutMapping("/preferences")
    @SaCheckLogin
    public ApiResponse<Void> updatePreferences(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        Map<String, Object> extInfo = parseExtInfo(user.getExtInfo());
        if (extInfo == null) {
            extInfo = new HashMap<>();
        }
        extInfo.putAll(body);
        user.setExtInfo(toJsonString(extInfo));
        sysUserService.updateUser(user);
        return ApiResponse.success();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    @SaCheckLogin
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用户 {} 上传头像: {} (大小: {} bytes)", userId, file.getOriginalFilename(), file.getSize());

        // 简单实现：返回占位URL，实际生产环境应使用OSS/MinIO
        String avatarUrl = "/api/profile/avatar/" + userId + "/" + file.getOriginalFilename();

        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setAvatar(avatarUrl);
            sysUserService.updateUser(user);
        }

        return ApiResponse.ok(avatarUrl);
    }

    private Map<String, Object> parseExtInfo(String extInfo) {
        if (extInfo == null || extInfo.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(extInfo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析 extInfo JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("序列化 extInfo 失败: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> getDefaultPreferences() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("language", "zh-CN");
        defaults.put("theme", "light");
        defaults.put("layoutMode", "side");
        defaults.put("primaryColor", "#1890ff");
        defaults.put("tagsView", true);
        defaults.put("fixedHeader", true);
        defaults.put("sidebarLogo", true);
        return defaults;
    }
}
