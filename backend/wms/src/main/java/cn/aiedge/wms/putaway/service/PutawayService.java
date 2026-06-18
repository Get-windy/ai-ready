package cn.aiedge.wms.putaway.service;

import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.entity.WmsPutawayDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PutawayService {
    boolean saveTask(WmsPutawayTask task);
    boolean updateTask(WmsPutawayTask task);
    WmsPutawayTask getTaskById(Long id);
    Page<WmsPutawayTask> pageTask(Page<WmsPutawayTask> page, WmsPutawayTask query);
    boolean removeTask(Long id);
    boolean saveDetail(WmsPutawayDetail detail);
    boolean updateDetail(WmsPutawayDetail detail);
    List<WmsPutawayDetail> listByTaskId(Long taskId);
    void startPutaway(Long taskId, Long userId, String userName);
    void confirmPutaway(Long taskId, Long userId, String userName);
}
