package cn.aiedge.wms.pick.service;

import cn.aiedge.wms.entity.WmsPickWave;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.entity.WmsPickDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PickService {
    // 波次
    WmsPickWave createWave(List<Long> saleOrderIds);
    boolean saveWave(WmsPickWave wave);
    boolean updateWave(WmsPickWave wave);
    WmsPickWave getWaveById(Long id);
    Page<WmsPickWave> pageWave(Page<WmsPickWave> page, WmsPickWave query);
    boolean removeWave(Long id);
    // 任务
    boolean saveTask(WmsPickTask task);
    boolean updateTask(WmsPickTask task);
    WmsPickTask getTaskById(Long id);
    Page<WmsPickTask> pageTask(Page<WmsPickTask> page, WmsPickTask query);
    List<WmsPickTask> listByWaveId(Long waveId);
    boolean removeTask(Long id);
    // 明细
    boolean saveDetail(WmsPickDetail detail);
    boolean updateDetail(WmsPickDetail detail);
    WmsPickDetail getDetailById(Long id);
    List<WmsPickDetail> listByTaskId(Long taskId);
    // 操作
    void startPick(Long taskId, Long userId, String userName);
    void confirmPickItem(Long detailId, BigDecimal pickedQuantity);
    void markShortage(Long detailId, BigDecimal shortageQuantity);
    void completePick(Long taskId);
}
