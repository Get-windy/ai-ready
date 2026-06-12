package cn.aiedge.wms.ship.service.impl;

import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.ship.mapper.WmsShipTaskMapper;
import cn.aiedge.wms.ship.mapper.WmsShipDetailMapper;
import cn.aiedge.wms.ship.service.ShipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipServiceImpl implements ShipService {

    private final WmsShipTaskMapper taskMapper;
    private final WmsShipDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsShipTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsShipTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsShipTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsShipTask> pageTask(Page<WmsShipTask> page, WmsShipTask query) {
        LambdaQueryWrapper<WmsShipTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsShipTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsShipTask::getTaskNo, query.getTaskNo());
            }
            if (query.getSourceOrderNo() != null) {
                wrapper.like(WmsShipTask::getSourceOrderNo, query.getSourceOrderNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsShipTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsShipTask::getStatus, query.getStatus());
            }
            if (query.getPickTaskId() != null) {
                wrapper.eq(WmsShipTask::getPickTaskId, query.getPickTaskId());
            }
        }
        wrapper.orderByDesc(WmsShipTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsShipDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsShipDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public List<WmsShipDetail> listByShipId(Long shipId) {
        LambdaQueryWrapper<WmsShipDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsShipDetail::getShipId, shipId);
        wrapper.orderByAsc(WmsShipDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startShip(Long taskId, Long userId, String userName) {
        WmsShipTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != WmsTaskStatus.PENDING) return;
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("发货任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanItem(Long detailId, BigDecimal scannedQuantity) {
        WmsShipDetail detail = detailMapper.selectById(detailId);
        if (detail != null && detail.getStatus() == 0) {
            detail.setScannedQuantity(scannedQuantity);
            detail.setStatus(1); // 已扫描
            detailMapper.updateById(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShip(Long taskId) {
        WmsShipTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != WmsTaskStatus.IN_PROGRESS) return;
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setShipTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("发货确认完成: taskId={}", taskId);
    }
}
