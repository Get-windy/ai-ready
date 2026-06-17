package cn.aiedge.module.service;

import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;

import java.util.List;
import java.util.Map;

/**
 * 模块管理服务接口
 */
public interface ModuleService {

    /**
     * 获取模块列表
     */
    List<SysModule> getModuleList();

    /**
     * 根据ID获取模块
     */
    SysModule getModuleById(Long id);

    /**
     * 创建模块
     */
    SysModule createModule(SysModule module);

    /**
     * 更新模块
     */
    SysModule updateModule(SysModule module);

    /**
     * 删除模块
     */
    boolean deleteModule(Long id);

    /**
     * 切换模块状态
     */
    boolean toggleStatus(Long id);

    /**
     * 获取版本列表
     */
    List<SysModuleVersion> getVersionList();

    /**
     * 发布版本
     */
    SysModuleVersion publishVersion(Long moduleId, String version, String changelog);

    /**
     * 获取使用统计
     */
    List<Map<String, Object>> getUsageStats();
}
