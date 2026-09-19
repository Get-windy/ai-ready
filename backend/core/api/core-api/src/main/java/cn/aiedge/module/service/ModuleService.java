package cn.aiedge.module.service;

import cn.aiedge.module.model.SysModule;
import cn.aiedge.module.model.SysModuleVersion;
import com.baomidou.mybatisplus.core.metadata.IPage;

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
     * 分页查询模块列表（支持按名称/编码模糊、按状态筛选，按 sort_order 升序）
     *
     * @param keyword  模块名称或模块编码（模糊匹配，可空）
     * @param status   状态：1=启用 0=停用（可空＝全部）
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     */
    IPage<SysModule> pageModules(String keyword, Integer status, int pageNum, int pageSize);

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
     * 分页查询版本列表（支持按模块 / 发布状态 / 版本号关键字筛选）
     * 排序为 `release_time DESC NULLS LAST`（未发布的草稿沉底）
     */
    IPage<SysModuleVersion> pageVersions(Long moduleId, String releaseStatus, String version,
                                         int pageNum, int pageSize);

    /**
     * 发布版本
     */
    SysModuleVersion publishVersion(Long moduleId, String version, String changelog);

    /**
     * 回滚：把模块的「当前版本号」回写为指定历史版本（不新增版本行）
     */
    boolean rollbackVersion(Long moduleId, String version);

    /**
     * 获取模块使用统计（含汇总卡数据）
     *
     * @param days 统计窗口天数（审计日志的活跃/调用口径都按该窗口）
     * @return {records: 逐模块统计, summary: 汇总卡, windowDays: 实际窗口, windowStart: 窗口起点}
     */
    Map<String, Object> getUsageStats(int days);
}
