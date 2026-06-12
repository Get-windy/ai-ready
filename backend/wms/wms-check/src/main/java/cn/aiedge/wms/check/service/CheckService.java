package cn.aiedge.wms.check.service;

import cn.aiedge.wms.entity.WmsCheckTask;
import cn.aiedge.wms.entity.WmsCheckResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface CheckService {
    boolean saveTask(WmsCheckTask task);
    boolean updateTask(WmsCheckTask task);
    WmsCheckTask getTaskById(Long id);
    Page<WmsCheckTask> pageTask(Page<WmsCheckTask> page, WmsCheckTask query);
    boolean removeTask(Long id);
    boolean saveResult(WmsCheckResult result);
    boolean updateResult(WmsCheckResult result);
    List<WmsCheckResult> listByTaskId(Long taskId);
    void startCheck(Long taskId, Long userId, String userName);
    void submitResult(Long taskId, Long userId, String userName);
    void approveCheck(Long taskId, Long userId);
}
