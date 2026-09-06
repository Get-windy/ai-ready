package cn.aiedge.wms.move.service.impl;

import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.inventory.service.InventoryService;
import cn.aiedge.wms.move.dto.MoveTaskQuery;
import cn.aiedge.wms.move.dto.MoveDetailVO;
import cn.aiedge.wms.move.mapper.WmsMoveTaskMapper;
import cn.aiedge.wms.move.mapper.WmsMoveDetailMapper;
import cn.aiedge.wms.move.service.MoveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoveServiceImpl implements MoveService {

    private final WmsMoveTaskMapper taskMapper;
    private final WmsMoveDetailMapper detailMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsMoveTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsMoveTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsMoveTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsMoveTask> pageTask(Page<WmsMoveTask> page, MoveTaskQuery query) {
        LambdaQueryWrapper<WmsMoveTask> wrapper = buildDocWrapper(query);
        wrapper.orderByDesc(WmsMoveTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsMoveDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsMoveDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public List<WmsMoveDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsMoveDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsMoveDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsMoveDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetails(Long taskId, List<WmsMoveDetail> details) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 先删后插（逻辑删除旧明细）
        LambdaQueryWrapper<WmsMoveDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsMoveDetail::getTaskId, taskId);
        detailMapper.delete(delWrapper);
        int lineNo = 1;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (WmsMoveDetail detail : details) {
            detail.setId(null);
            detail.setTaskId(taskId);
            detail.setLineNo(lineNo++);
            if (detail.getStatus() == null) detail.setStatus(0);
            detailMapper.insert(detail);
            if (detail.getQuantity() != null) {
                totalQuantity = totalQuantity.add(detail.getQuantity());
            }
        }
        // 回写头表明细数/合计量
        task.setTotalItems(details.size());
        task.setTotalQuantity(totalQuantity);
        taskMapper.updateById(task);
        log.info("移库明细保存: taskId={}, items={}, totalQuantity={}", taskId, details.size(), totalQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMove(Long taskId, Long userId, String userName) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("移库任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeMove(Long taskId, Long userId, String userName) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        if (task.getWarehouseId() == null) {
            throw new WmsBusinessException("移库任务未指定仓库，无法执行移库");
        }
        List<WmsMoveDetail> details = listByTaskId(taskId);
        if (details.isEmpty()) {
            throw new WmsBusinessException("移库明细为空，无法执行移库");
        }
        // 序列号移动校验：同一移库单内序列号唯一（批次移动校验由 InventoryService.move 按批次核库存）
        Set<String> serialNoSet = new HashSet<>();
        for (WmsMoveDetail d : details) {
            if (d.getSerialNo() != null && !d.getSerialNo().isBlank()) {
                if (!serialNoSet.add(d.getSerialNo().trim())) {
                    throw new WmsBusinessException("移库明细存在重复序列号[" + d.getSerialNo().trim() + "]");
                }
            }
        }
        BigDecimal movedQty = BigDecimal.ZERO;
        for (WmsMoveDetail detail : details) {
            if (detail.getProductId() == null || detail.getQuantity() == null
                    || detail.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new WmsBusinessException("移库明细存在未选择商品或移库数量<=0的记录，无法执行移库");
            }
            if (detail.getFromLocationId() == null || detail.getToLocationId() == null) {
                throw new WmsBusinessException("移库明细存在未设置源/目标货位的记录，无法执行移库");
            }
            if (detail.getFromLocationId().equals(detail.getToLocationId())) {
                throw new WmsBusinessException("移库明细源货位与目标货位相同，无法执行移库");
            }
            // 双货位库存移动：源货位扣减 + 目标货位增加 + 库存日志
            inventoryService.move(detail.getProductId(), task.getWarehouseId(),
                    detail.getFromLocationId(), detail.getToLocationId(),
                    detail.getBatchNo(), detail.getQuantity(),
                    task.getTaskNo(), "move", taskId, userId, userName);
            // 明细置为已移库
            detail.setStatus(1);
            detailMapper.updateById(detail);
            movedQty = movedQty.add(detail.getQuantity());
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        task.setMovedQuantity(movedQty);
        taskMapper.updateById(task);
        log.info("移库任务完成: taskId={}, userId={}, movedQuantity={}", taskId, userId, movedQty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMove(Long taskId, String reason) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.PENDING && task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw new WmsBusinessException("移库任务[" + task.getTaskNo() + "]当前状态[" + task.getStatus() + "]不允许取消");
        }
        task.setStatus(WmsTaskStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            task.setRemark(reason);
        }
        taskMapper.updateById(task);
        log.info("移库任务取消: taskId={}, reason={}", taskId, reason);
    }

    @Override
    public String nextNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "MV-" + date + rand;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsMoveTask createMoveFromStocktake(String sourceNo, Long warehouseId, String warehouseName, List<WmsMoveDetail> details) {
        WmsMoveTask task = new WmsMoveTask();
        task.setTaskNo(nextNo());
        task.setWarehouseId(warehouseId);
        task.setWarehouseName(warehouseName);
        task.setSourceType(1); // 盘点差异
        task.setSourceNo(sourceNo);
        task.setMoveType(1);
        task.setStatus(WmsTaskStatus.PENDING);
        taskMapper.insert(task);
        if (details != null && !details.isEmpty()) {
            saveDetails(task.getId(), details);
        }
        log.info("盘点货位转移生成移库单: taskId={}, taskNo={}, sourceNo={}", task.getId(), task.getTaskNo(), sourceNo);
        return task;
    }

    @Override
    public Page<MoveDetailVO> pageDetail(Page<MoveDetailVO> page, MoveTaskQuery query) {
        // 1. 按单据级条件筛选命中的单据ID
        LambdaQueryWrapper<WmsMoveTask> docWrapper = buildDocWrapper(query);
        docWrapper.select(WmsMoveTask::getId);
        List<Long> docIds = taskMapper.selectList(docWrapper).stream()
                .map(WmsMoveTask::getId).filter(Objects::nonNull).toList();
        if (docIds.isEmpty()) {
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        }
        // 2. 明细分页：明细 in 命中单据 + 明细级过滤
        LambdaQueryWrapper<WmsMoveDetail> dw = new LambdaQueryWrapper<>();
        dw.in(WmsMoveDetail::getTaskId, docIds);
        if (query != null && query.getProductName() != null && !query.getProductName().isBlank()) {
            dw.like(WmsMoveDetail::getProductName, query.getProductName());
        }
        if (query != null && query.getItemRemark() != null && !query.getItemRemark().isBlank()) {
            dw.like(WmsMoveDetail::getRemark, query.getItemRemark());
        }
        dw.orderByAsc(WmsMoveDetail::getTaskId).orderByAsc(WmsMoveDetail::getLineNo);
        Page<WmsMoveDetail> detailPage = new Page<>(page.getCurrent(), page.getSize());
        detailMapper.selectPage(detailPage, dw);
        // 3. 批量补齐单头字段
        Set<Long> taskIds = detailPage.getRecords().stream()
                .map(WmsMoveDetail::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsMoveTask> docMap = taskIds.isEmpty() ? Map.of()
                : taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(WmsMoveTask::getId, t -> t));
        List<MoveDetailVO> vos = detailPage.getRecords().stream()
                .map(d -> toVO(d, docMap.get(d.getTaskId()))).toList();
        page.setRecords(vos);
        page.setTotal(detailPage.getTotal());
        return page;
    }

    /**
     * 单据级查询条件（按单据/按明细共用）.
     */
    private LambdaQueryWrapper<WmsMoveTask> buildDocWrapper(MoveTaskQuery q) {
        LambdaQueryWrapper<WmsMoveTask> w = new LambdaQueryWrapper<>();
        if (q == null) return w;
        String kw = q.getKeyword() != null && !q.getKeyword().isBlank() ? q.getKeyword().trim() : q.getTaskNo();
        if (kw != null && !kw.isBlank()) {
            w.like(WmsMoveTask::getTaskNo, kw);
        }
        if (q.getWarehouseId() != null) w.eq(WmsMoveTask::getWarehouseId, q.getWarehouseId());
        if (q.getStatus() != null) w.eq(WmsMoveTask::getStatus, q.getStatus());
        if (q.getMoveType() != null) w.eq(WmsMoveTask::getMoveType, q.getMoveType());
        if (q.getSourceType() != null) w.eq(WmsMoveTask::getSourceType, q.getSourceType());
        if (q.getSourceNo() != null && !q.getSourceNo().isBlank()) w.like(WmsMoveTask::getSourceNo, q.getSourceNo());
        if (q.getFromLocationCode() != null && !q.getFromLocationCode().isBlank()) {
            w.like(WmsMoveTask::getFromLocationCode, q.getFromLocationCode());
        }
        if (q.getToLocationCode() != null && !q.getToLocationCode().isBlank()) {
            w.like(WmsMoveTask::getToLocationCode, q.getToLocationCode());
        }
        if (q.getAssigneeName() != null && !q.getAssigneeName().isBlank()) {
            w.like(WmsMoveTask::getAssigneeName, q.getAssigneeName());
        }
        if (q.getDateStart() != null) w.ge(WmsMoveTask::getCreateTime, q.getDateStart().atStartOfDay());
        if (q.getDateEnd() != null) w.lt(WmsMoveTask::getCreateTime, q.getDateEnd().plusDays(1).atStartOfDay());
        return w;
    }

    private MoveDetailVO toVO(WmsMoveDetail d, WmsMoveTask t) {
        MoveDetailVO vo = new MoveDetailVO();
        vo.setId(d.getId());
        vo.setTaskId(d.getTaskId());
        vo.setLineNo(d.getLineNo());
        vo.setProductId(d.getProductId());
        vo.setProductCode(d.getProductCode());
        vo.setProductName(d.getProductName());
        vo.setProductSpec(d.getProductSpec());
        vo.setProductUnit(d.getProductUnit());
        vo.setQuantity(d.getQuantity());
        vo.setBatchNo(d.getBatchNo());
        vo.setSerialNo(d.getSerialNo());
        vo.setFromLocationId(d.getFromLocationId());
        vo.setFromLocationCode(d.getFromLocationCode());
        vo.setToLocationId(d.getToLocationId());
        vo.setToLocationCode(d.getToLocationCode());
        vo.setItemStatus(d.getStatus());
        vo.setItemRemark(d.getRemark());
        if (t != null) {
            vo.setTaskNo(t.getTaskNo());
            vo.setWarehouseName(t.getWarehouseName());
            vo.setStatus(t.getStatus());
            vo.setMoveType(t.getMoveType());
            vo.setSourceNo(t.getSourceNo());
            vo.setAssigneeName(t.getAssigneeName());
            vo.setDocRemark(t.getRemark());
            vo.setCreateTime(t.getCreateTime());
        }
        return vo;
    }
}
