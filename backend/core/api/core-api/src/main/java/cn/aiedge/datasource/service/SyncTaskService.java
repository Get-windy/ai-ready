package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.SyncTask;

import java.util.List;
import java.util.Map;

/**
 * 同步任务服务接口
 */
public interface SyncTaskService {

    List<SyncTask> list(Long tenantId);

    SyncTask create(SyncTask syncTask, Long tenantId, String createBy);

    SyncTask update(Long id, SyncTask syncTask, Long tenantId, String updateBy);

    boolean delete(Long id);

    /**
     * 立即执行同步任务
     *
     * <p>语义（本轮 2026-09-19 重写）：把任务投递给**已有的** {@code cn.aiedge.integration}
     * 同步引擎通道（不新建第二套 HTTP 客户端，见同步任务开发文档 §8.6 复用约束），
     * 并把投递结论如实写回 {@code last_run_*}。
     *
     * <p>🔴 三条诚实性约束：
     * <ol>
     *   <li>引擎不可达 / 没有可投递的引擎配置 → {@code success=false} + 明确原因，**不谎报已触发**；</li>
     *   <li>即使投递成功，只报「已投递」，**绝不报「已同步」** —— 引擎侧的数据搬运结果本系统无回读通道；</li>
     *   <li>{@code status}（启停开关）**不被本方法修改**；在途标记写在 {@code last_run_status} 上，
     *       且必然被复位为终态，不存在「永停 running」。</li>
     * </ol>
     *
     * @return {@code {success, message, dispatched, configId, ...}}
     */
    Map<String, Object> execute(Long id, Long tenantId, String operator);
}
