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
    void startMove(Long taskId, Long userId, String userName);
    void executeMove(Long taskId, Long userId, String userName);
}
