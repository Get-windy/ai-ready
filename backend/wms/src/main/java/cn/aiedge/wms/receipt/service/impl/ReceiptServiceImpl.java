package cn.aiedge.wms.receipt.service.impl;

import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.receipt.mapper.WmsReceiptTaskMapper;
import cn.aiedge.wms.receipt.mapper.WmsReceiptDetailMapper;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private final WmsReceiptTaskMapper taskMapper;
    private final WmsReceiptDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsReceiptTask createFromPurchaseOrder(Long purchaseOrderId) {
        // TODO: 从采购订单获取数据并创建收货任务
        // 此处仅创建空壳任务，实际业务需从采购模块查询
        WmsReceiptTask task = new WmsReceiptTask();
        task.setSourceType(1); // 采购入库
        task.setSourceOrderId(purchaseOrderId);
        task.setStatus(WmsTaskStatus.PENDING);
        taskMapper.insert(task);
        log.info("创建收货任务: id={}, purchaseOrderId={}", task.getId(), purchaseOrderId);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsReceiptTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsReceiptTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsReceiptTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public WmsReceiptTask getByTaskNo(String taskNo) {
        LambdaQueryWrapper<WmsReceiptTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsReceiptTask::getTaskNo, taskNo);
        return taskMapper.selectOne(wrapper);
    }

    @Override
    public Page<WmsReceiptTask> pageTask(Page<WmsReceiptTask> page, WmsReceiptTask query) {
        LambdaQueryWrapper<WmsReceiptTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsReceiptTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsReceiptTask::getTaskNo, query.getTaskNo());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsReceiptTask::getSourceType, query.getSourceType());
            }
            if (query.getSourceOrderNo() != null) {
                wrapper.like(WmsReceiptTask::getSourceOrderNo, query.getSourceOrderNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsReceiptTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsReceiptTask::getStatus, query.getStatus());
            }
            if (query.getPriority() != null) {
                wrapper.eq(WmsReceiptTask::getPriority, query.getPriority());
            }
        }
        wrapper.orderByDesc(WmsReceiptTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsReceiptDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsReceiptDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public WmsReceiptDetail getDetailById(Long id) {
        return detailMapper.selectById(id);
    }

    @Override
    public List<WmsReceiptDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsReceiptDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsReceiptDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsReceiptDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReceipt(Long taskId, Long userId, String userName) {
        WmsReceiptTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "收货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("收货任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long taskId, Long userId, String userName) {
        WmsReceiptTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "收货");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("收货任务完成: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReceipt(Long taskId, String reason) {
        WmsReceiptTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "收货");
        if (task.getStatus() != WmsTaskStatus.PENDING
                && task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.CANCELLED);
        task.setRemark(reason);
        taskMapper.updateById(task);
        log.info("收货任务取消: taskId={}, reason={}", taskId, reason);
    }
}
