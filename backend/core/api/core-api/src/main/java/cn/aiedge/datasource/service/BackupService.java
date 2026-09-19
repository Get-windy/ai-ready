package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.BackupRecord;

import java.util.List;
import java.util.Map;

/**
 * 备份服务接口
 *
 * <p>返回 {@code Map} 而不是 {@code boolean}：备份/恢复的结论不是一个布尔值能表达的 ——
 * 调用方必须能拿到「为什么没做」「做到哪一步」「产物在哪」，否则页面只能猜，
 * 而猜的结果就是上一版那种「谎报成功」。
 *
 * <p>所有返回值都以 {@code success} 布尔字段开头，失败时带 {@code message}（可直接展示）。
 */
public interface BackupService {

    /** 备份台账列表（dataSourceId 下推、tenantId 显式过滤） */
    List<BackupRecord> list(Long dataSourceId, Long tenantId);

    /**
     * 创建备份（**异步**执行 pg_dump）
     *
     * @return {@code {success, message, data:BackupRecord, target}} —— 返回即「已受理」，
     *         实际结果由后台线程回写台账，页面回读查看
     */
    Map<String, Object> create(Long dataSourceId, String backupName, String backupType, Long tenantId, String createBy);

    /**
     * 从备份恢复（**同步**执行 pg_restore，最高危动作）
     *
     * @param confirm 必须显式为 true，缺省（false）一律拒绝 —— 二次确认闸门
     */
    Map<String, Object> restore(Long id, boolean confirm, Long tenantId, String operator);

    /** 删除台账记录，并尽力删除对应的磁盘备份文件 */
    Map<String, Object> delete(Long id, Long tenantId);
}
