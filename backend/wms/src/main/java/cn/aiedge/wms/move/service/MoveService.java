package cn.aiedge.wms.move.service;

import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.entity.WmsMoveDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface MoveService {
    boolean saveTask(WmsMoveTask task);
    boolean updateTask(WmsMoveTask task);
    WmsMoveTask getTaskById(Long id);
    Page<WmsMoveTask> pageTask(Page<WmsMoveTask> page, WmsMoveTask query);
    boolean removeTask(Long id);
    boolean saveDetail(WmsMoveDetail detail);
    boolean updateDetail(WmsMoveDetail detail);
    List<WmsMoveDetail> listByTaskId(Long taskId);
    // 明细整体保存（先删后插，仅待处理状态可操作）
    void saveDetails(Long taskId, List<WmsMoveDetail> details);
    void startMove(Long taskId, Long userId, String userName);
    void executeMove(Long taskId, Long userId, String userName);
}
