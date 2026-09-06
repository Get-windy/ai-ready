package cn.aiedge.wms.putaway.service;

import cn.aiedge.wms.controller.dto.WmsPutawayDetailQuery;
import cn.aiedge.wms.controller.dto.WmsPutawayDetailVO;
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
    // 明细整体保存（先删后插，仅待处理状态可操作）
    void saveDetails(Long taskId, List<WmsPutawayDetail> details);
    void startPutaway(Long taskId, Long userId, String userName);
    void confirmPutaway(Long taskId, Long userId, String userName);
    void cancelPutaway(Long taskId, String reason);
    Page<WmsPutawayDetailVO> pageDetail(Page<WmsPutawayDetail> page, WmsPutawayDetailQuery query);
}
