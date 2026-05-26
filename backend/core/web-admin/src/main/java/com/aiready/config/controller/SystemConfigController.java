package com.aiready.config.controller;

import com.aiready.config.dto.*;
import com.aiready.config.entity.ConfigAuditLog;
import com.aiready.config.service.ConfigAuditService;
import com.aiready.config.service.ConfigGroupService;
import com.aiready.config.service.ConfigVersionService;
import com.aiready.config.service.SystemConfigService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "系统配置管理", description = "系统配置管理接口")
public class SystemConfigController {
    
    private final SystemConfigService configService;
    private final ConfigGroupService groupService;
    private final ConfigVersionService versionService;
    private final ConfigAuditService auditService;
    
    /**
     * 获取配置值
     */
    @GetMapping("/value/{configKey}")
    @Operation(summary = "获取配置值")
    public String getConfigValue(@PathVariable String configKey) {
        return configService.getConfigValue(configKey);
    }
    
    /**
     * 获取配置详情
     */
    @GetMapping("/detail/{configKey}")
    @Operation(summary = "获取配置详情")
    public SystemConfigDTO getConfigByKey(@PathVariable String configKey) {
        return configService.getConfigByKey(configKey);
    }
    
    /**
     * 查询配置列表
     */
    @PostMapping("/list")
    @Operation(summary = "查询配置列表")
    public IPage<SystemConfigDTO> queryConfigs(@RequestBody ConfigQueryRequest request) {
        return configService.queryConfigs(request);
    }
    
    /**
     * 创建配置
     */
    @PostMapping("/create")
    @Operation(summary = "创建配置")
    public SystemConfigDTO saveConfig(@RequestBody ConfigSaveRequest request, 
                                       @RequestParam Long operatorId) {
        return configService.saveConfig(request, operatorId);
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/update/{configId}")
    @Operation(summary = "更新配置")
    public SystemConfigDTO updateConfig(@PathVariable Long configId,
                                         @RequestBody ConfigSaveRequest request,
                                         @RequestParam Long operatorId) {
        return configService.updateConfig(configId, request, operatorId);
    }
    
    /**
     * 热更新配置值
     */
    @PatchMapping("/value/{configId}")
    @Operation(summary = "热更新配置值")
    public SystemConfigDTO updateConfigValue(@PathVariable Long configId,
                                              @RequestParam String value,
                                              @RequestParam(required = false) String changeReason,
                                              @RequestParam Long operatorId) {
        return configService.updateConfigValue(configId, value, changeReason, operatorId);
    }
    
    /**
     * 批量更新配置
     */
    @PostMapping("/batch-update")
    @Operation(summary = "批量更新配置")
    public void batchUpdate(@RequestBody BatchUpdateRequest request,
                            @RequestParam Long operatorId) {
        configService.batchUpdate(request, operatorId);
    }
    
    /**
     * 删除配置
     */
    @DeleteMapping("/delete/{configId}")
    @Operation(summary = "删除配置")
    public void deleteConfig(@PathVariable Long configId, @RequestParam Long operatorId) {
        configService.deleteConfig(configId, operatorId);
    }
    
    /**
     * 批量删除配置
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除配置")
    public void batchDelete(@RequestBody List<Long> configIds, @RequestParam Long operatorId) {
        configService.batchDelete(configIds, operatorId);
    }
    
    /**
     * 获取组内配置
     */
    @GetMapping("/group/{groupCode}/configs")
    @Operation(summary = "获取组内配置")
    public List<SystemConfigDTO> getConfigsByGroup(@PathVariable String groupCode) {
        return configService.getConfigsByGroup(groupCode);
    }
    
    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新配置缓存")
    public void refreshCache() {
        configService.refreshCache();
    }
    
    /**
     * 导出配置
     */
    @PostMapping("/export")
    @Operation(summary = "导出配置")
    public String exportConfigs(@RequestBody ConfigImportExportDTO request) {
        return configService.exportConfigs(request);
    }
    
    /**
     * 导入配置
     */
    @PostMapping("/import")
    @Operation(summary = "导入配置")
    public void importConfigs(@RequestBody ConfigImportExportDTO request, @RequestParam Long operatorId) {
        configService.importConfigs(request, operatorId);
    }
    
    /**
     * 回滚配置
     */
    @PostMapping("/rollback/{configId}")
    @Operation(summary = "回滚到指定版本")
    public SystemConfigDTO rollbackToVersion(@PathVariable Long configId,
                                              @RequestParam Integer version,
                                              @RequestParam Long operatorId) {
        return configService.rollbackToVersion(configId, version, operatorId);
    }
    
    // ==================== 配置组接口 ====================
    
    /**
     * 创建配置组
     */
    @PostMapping("/group/create")
    @Operation(summary = "创建配置组")
    public ConfigGroupDTO createGroup(@RequestBody ConfigGroupRequest request, @RequestParam Long operatorId) {
        return groupService.createGroup(request, operatorId);
    }
    
    /**
     * 更新配置组
     */
    @PutMapping("/group/update/{groupId}")
    @Operation(summary = "更新配置组")
    public ConfigGroupDTO updateGroup(@PathVariable Long groupId,
                                       @RequestBody ConfigGroupRequest request,
                                       @RequestParam Long operatorId) {
        return groupService.updateGroup(groupId, request, operatorId);
    }
    
    /**
     * 删除配置组
     */
    @DeleteMapping("/group/delete/{groupId}")
    @Operation(summary = "删除配置组")
    public void deleteGroup(@PathVariable Long groupId, @RequestParam Long operatorId) {
        groupService.deleteGroup(groupId, operatorId);
    }
    
    /**
     * 获取配置组详情
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "获取配置组详情")
    public ConfigGroupDTO getGroupById(@PathVariable Long groupId) {
        return groupService.getGroupById(groupId);
    }
    
    /**
     * 获取所有配置组（树形）
     */
    @GetMapping("/groups")
    @Operation(summary = "获取所有配置组")
    public List<ConfigGroupDTO> getAllGroups() {
        return groupService.getAllGroups();
    }
    
    /**
     * 移动配置组
     */
    @PostMapping("/group/move/{groupId}")
    @Operation(summary = "移动配置组")
    public ConfigGroupDTO moveGroup(@PathVariable Long groupId,
                                     @RequestParam Long newParentId,
                                     @RequestParam Long operatorId) {
        return groupService.moveGroup(groupId, newParentId, operatorId);
    }
    
    // ==================== 版本管理接口 ====================
    
    /**
     * 获取配置版本历史
     */
    @GetMapping("/versions/{configId}")
    @Operation(summary = "获取配置版本历史")
    public List<ConfigVersionDTO> getVersions(@PathVariable Long configId) {
        return versionService.getVersionsByConfigId(configId);
    }
    
    /**
     * 对比版本
     */
    @GetMapping("/compare/{configId}")
    @Operation(summary = "对比两个版本")
    public String compareVersions(@PathVariable Long configId,
                                   @RequestParam Integer version1,
                                   @RequestParam Integer version2) {
        return versionService.compareVersions(configId, version1, version2);
    }
    
    // ==================== 审计日志接口 ====================
    
    /**
     * 获取配置审计日志
     */
    @GetMapping("/audit/{configId}")
    @Operation(summary = "获取配置审计日志")
    public IPage<ConfigAuditLog> getAuditLogs(@PathVariable Long configId,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer size) {
        return auditService.getAuditLogsByConfigId(configId, page, size);
    }
}
