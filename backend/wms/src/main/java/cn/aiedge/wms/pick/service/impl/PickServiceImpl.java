package cn.aiedge.wms.pick.service.impl;

import cn.aiedge.wms.entity.WmsPickWave;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.entity.WmsPickDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.pick.mapper.WmsPickWaveMapper;
import cn.aiedge.wms.pick.mapper.WmsPickTaskMapper;
import cn.aiedge.wms.pick.mapper.WmsPickDetailMapper;
import cn.aiedge.wms.pick.service.PickService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickServiceImpl implements PickService {

    private final WmsPickWaveMapper waveMapper;
    private final WmsPickTaskMapper taskMapper;
    private final WmsPickDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsPickWave createWave(List<Long> saleOrderIds) {
        // TODO: 从销售订单获取数据并创建拣货波次
        WmsPickWave wave = new WmsPickWave();
        wave.setStatus(0); // 待分配
        waveMapper.insert(wave);
        return wave;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWave(WmsPickWave wave) {
        return waveMapper.insert(wave) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWave(WmsPickWave wave) {
        return waveMapper.updateById(wave) > 0;
    }

    @Override
    public WmsPickWave getWaveById(Long id) {
        return waveMapper.selectById(id);
    }

    @Override
    public Page<WmsPickWave> pageWave(Page<WmsPickWave> page, WmsPickWave query) {
        LambdaQueryWrapper<WmsPickWave> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsPickWave::getId, query.getId());
            }
            if (query.getWaveNo() != null) {
                wrapper.like(WmsPickWave::getWaveNo, query.getWaveNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsPickWave::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsPickWave::getStatus, query.getStatus());
            }
            if (query.getPriority() != null) {
                wrapper.eq(WmsPickWave::getPriority, query.getPriority());
            }
            if (query.getWaveType() != null) {
                wrapper.eq(WmsPickWave::getWaveType, query.getWaveType());
            }
        }
        wrapper.orderByDesc(WmsPickWave::getId);
        return waveMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWave(Long id) {
        return waveMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsPickTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsPickTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsPickTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsPickTask> pageTask(Page<WmsPickTask> page, WmsPickTask query) {
        LambdaQueryWrapper<WmsPickTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsPickTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsPickTask::getTaskNo, query.getTaskNo());
            }
            if (query.getWaveId() != null) {
                wrapper.eq(WmsPickTask::getWaveId, query.getWaveId());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsPickTask::getSourceType, query.getSourceType());
            }
            if (query.getSourceOrderNo() != null) {
                wrapper.like(WmsPickTask::getSourceOrderNo, query.getSourceOrderNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsPickTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsPickTask::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsPickTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WmsPickTask> listByWaveId(Long waveId) {
        LambdaQueryWrapper<WmsPickTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsPickTask::getWaveId, waveId);
        wrapper.orderByDesc(WmsPickTask::getId);
        return taskMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsPickDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsPickDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public WmsPickDetail getDetailById(Long id) {
        return detailMapper.selectById(id);
    }

    @Override
    public List<WmsPickDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsPickDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsPickDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsPickDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPick(Long taskId, Long userId, String userName) {
        WmsPickTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "拣货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("拣货任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickItem(Long detailId, BigDecimal pickedQuantity) {
        WmsPickDetail detail = detailMapper.selectById(detailId);
        if (detail == null) throw WmsBusinessException.taskNotFound(detailId);
        if (detail.getStatus() != 0) {
            throw new WmsBusinessException(String.format("拣货明细[%d]状态不允许操作，当前状态[%d]", detailId, detail.getStatus()));
        }
        detail.setPickedQuantity(pickedQuantity);
        detail.setStatus(1); // 已拣货
        detailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markShortage(Long detailId, BigDecimal shortageQuantity) {
        WmsPickDetail detail = detailMapper.selectById(detailId);
        if (detail == null) throw WmsBusinessException.taskNotFound(detailId);
        if (detail.getStatus() != 0) {
            throw new WmsBusinessException(String.format("拣货明细[%d]状态不允许操作，当前状态[%d]", detailId, detail.getStatus()));
        }
        detail.setShortageQuantity(shortageQuantity);
        detail.setPickedQuantity(detail.getExpectedQuantity().subtract(shortageQuantity));
        detail.setStatus(2); // 缺货
        detailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePick(Long taskId) {
        WmsPickTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "拣货");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        taskMapper.updateById(task);
        log.info("拣货任务完成: taskId={}", taskId);
    }
}
