package cn.aiedge.wms.pick.service.impl;

import cn.aiedge.wms.entity.WmsPickWave;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.entity.WmsPickDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.pick.mapper.WmsPickWaveMapper;
import cn.aiedge.wms.pick.mapper.WmsPickTaskMapper;
import cn.aiedge.wms.pick.dto.PickTaskQuery;
import cn.aiedge.wms.pick.dto.PickTaskDetailVO;
import cn.aiedge.wms.inventory.service.InventoryService;
import cn.aiedge.wms.pick.mapper.WmsPickDetailMapper;
import cn.aiedge.wms.pick.service.PickService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickServiceImpl implements PickService {

    /** 拣货任务状态机：0待拣货 / 1拣货中 / 2已完成 / 3缺货 / 4已取消（区别于通用 WmsTaskStatus 的 3=已取消/4=异常） */
    private static final int TASK_CANCELLED = 4;
    /** 拣货单号前缀 */
    private static final String NO_PREFIX = "PK-";

    private final WmsPickWaveMapper waveMapper;
    private final WmsPickTaskMapper taskMapper;
    private final WmsPickDetailMapper detailMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsPickWave createWave(List<Long> saleOrderIds) {
        WmsPickWave wave = new WmsPickWave();
        wave.setWaveNo("WV-" + System.currentTimeMillis());
        wave.setStatus(0); // 待分配
        wave.setWaveType(1); // 按单波次
        wave.setPriority(1); // 普通
        wave.setOrderCount(saleOrderIds.size());
        wave.setItemCount(0);
        wave.setTotalQuantity(java.math.BigDecimal.ZERO);
        wave.setPickedQuantity(java.math.BigDecimal.ZERO);
        wave.setRemark("由销售订单自动创建，订单数: " + saleOrderIds.size());
        waveMapper.insert(wave);
        log.info("创建拣货波次: waveId={}, waveNo={}, saleOrderIds={}", wave.getId(), wave.getWaveNo(), saleOrderIds);
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
            // 单号/来源单号关键字模糊（Controller 将 keyword 注入 taskNo 与 sourceOrderNo，or 匹配）
            boolean hasTaskNo = StringUtils.hasText(query.getTaskNo());
            boolean hasSourceOrderNo = StringUtils.hasText(query.getSourceOrderNo());
            if (hasTaskNo || hasSourceOrderNo) {
                wrapper.and(w -> {
                    if (hasTaskNo) w.like(WmsPickTask::getTaskNo, query.getTaskNo());
                    if (hasSourceOrderNo) w.like(WmsPickTask::getSourceOrderNo, query.getSourceOrderNo());
                });
            }
            if (query.getWaveId() != null) {
                wrapper.eq(WmsPickTask::getWaveId, query.getWaveId());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsPickTask::getSourceType, query.getSourceType());
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
    public void saveDetails(Long taskId, List<WmsPickDetail> details) {
        WmsPickTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "拣货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 先删后插（逻辑删除旧明细）
        LambdaQueryWrapper<WmsPickDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsPickDetail::getTaskId, taskId);
        detailMapper.delete(delWrapper);
        int lineNo = 1;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (WmsPickDetail detail : details) {
            if (detail.getProductId() == null) {
                throw new WmsBusinessException("拣货明细第" + lineNo + "行请选择商品");
            }
            if (detail.getExpectedQuantity() == null || detail.getExpectedQuantity().signum() <= 0) {
                throw new WmsBusinessException("拣货明细第" + lineNo + "行应拣数量必须大于0");
            }
            detail.setId(null);
            detail.setTaskId(taskId);
            detail.setLineNo(lineNo++);
            if (detail.getStatus() == null) detail.setStatus(0);
            detailMapper.insert(detail);
            if (detail.getExpectedQuantity() != null) {
                totalQuantity = totalQuantity.add(detail.getExpectedQuantity());
            }
        }
        // 回写头表明细数/合计量
        task.setTotalItems(details.size());
        task.setTotalQuantity(totalQuantity);
        taskMapper.updateById(task);
        log.info("拣货明细保存: taskId={}, items={}, totalQuantity={}", taskId, details.size(), totalQuantity);
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
        // 拣货开始：冻结明细应拣量（预留库存，防并发超拣；最终扣减由销售出库单 ship 统一执行）
        freezeTaskDetails(task, userId, userName);
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
        BigDecimal expected = detail.getExpectedQuantity() != null ? detail.getExpectedQuantity() : BigDecimal.ZERO;
        if (shortageQuantity.compareTo(expected) > 0) {
            throw new WmsBusinessException(String.format("拣货明细[%d]缺货数量[%s]不能大于应拣数量[%s]", detailId, shortageQuantity, expected));
        }
        detail.setShortageQuantity(shortageQuantity);
        detail.setPickedQuantity(expected.subtract(shortageQuantity));
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
        // 拣货完成：释放冻结并按实拣量统一扣减（erp_stock 仅在此扣一次，禁双扣）
        settleTaskDetails(task, true);
        log.info("拣货任务完成: taskId={}", taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, String reason) {
        WmsPickTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "拣货");
        if (task.getStatus() == WmsTaskStatus.COMPLETED || task.getStatus() == TASK_CANCELLED) {
            throw new WmsBusinessException(String.format("拣货任务[%s]当前状态[%d]不允许取消", task.getTaskNo(), task.getStatus()));
        }
        task.setStatus(TASK_CANCELLED);
        if (reason != null && !reason.isBlank()) {
            task.setRemark(reason);
        }
        taskMapper.updateById(task);
        // 作废：释放明细冻结库存（不扣减），避免库存被长期锁死
        settleTaskDetails(task, false);
        log.info("拣货任务取消: taskId={}, reason={}", taskId, reason);
    }

    // ══════════ 金标准查询 / 编号 ══════════

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /** 从查询条件构建单据级过滤 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<WmsPickTask> buildTaskWrapper(PickTaskQuery q) {
        LambdaQueryWrapper<WmsPickTask> wrapper = new LambdaQueryWrapper<>();
        if (q == null) return wrapper;
        // 关键字：单号/来源单号模糊（OR）
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword();
            wrapper.and(w -> w.like(WmsPickTask::getTaskNo, kw).or().like(WmsPickTask::getSourceOrderNo, kw));
        }
        if (StringUtils.hasText(q.getTaskNo()) && !StringUtils.hasText(q.getKeyword())) {
            wrapper.like(WmsPickTask::getTaskNo, q.getTaskNo());
        }
        if (StringUtils.hasText(q.getSourceOrderNo()) && !StringUtils.hasText(q.getKeyword())) {
            wrapper.like(WmsPickTask::getSourceOrderNo, q.getSourceOrderNo());
        }
        wrapper.like(StringUtils.hasText(q.getCustomerName()), WmsPickTask::getCustomerName, q.getCustomerName())
                .like(StringUtils.hasText(q.getWarehouseName()), WmsPickTask::getWarehouseName, q.getWarehouseName())
                .like(StringUtils.hasText(q.getRemark()), WmsPickTask::getRemark, q.getRemark())
                .eq(q.getWarehouseId() != null, WmsPickTask::getWarehouseId, q.getWarehouseId())
                .eq(q.getSourceType() != null, WmsPickTask::getSourceType, q.getSourceType())
                .eq(q.getStatus() != null, WmsPickTask::getStatus, q.getStatus())
                .eq(q.getPriority() != null, WmsPickTask::getPriority, q.getPriority());
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(WmsPickTask::getCreateTime, LocalDate.parse(q.getDateStart()).atStartOfDay());
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(WmsPickTask::getCreateTime, LocalDate.parse(q.getDateEnd()).plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(WmsPickTask::getId);
        return wrapper;
    }

    @Override
    public Page<WmsPickTask> pageOrderByQuery(PickTaskQuery query) {
        return taskMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), buildTaskWrapper(query));
    }

    @Override
    public Page<PickTaskDetailVO> pageDetail(PickTaskQuery query) {
        // 1. 先取满足单据级过滤的任务ID集合
        LambdaQueryWrapper<WmsPickTask> docWrapper = buildTaskWrapper(query);
        docWrapper.select(WmsPickTask::getId);
        List<Long> taskIds = taskMapper.selectList(docWrapper).stream()
                .map(WmsPickTask::getId).collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤 + 分页
        LambdaQueryWrapper<WmsPickDetail> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(WmsPickDetail::getTaskId, taskIds)
                .like(StringUtils.hasText(query.getProductName()), WmsPickDetail::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getLocationCode()), WmsPickDetail::getLocationCode, query.getLocationCode())
                .like(StringUtils.hasText(query.getItemRemark()), WmsPickDetail::getRemark, query.getItemRemark())
                .orderByAsc(WmsPickDetail::getLineNo);
        Page<WmsPickDetail> itemPage = detailMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageTaskIds = itemPage.getRecords().stream()
                .map(WmsPickDetail::getTaskId).distinct().collect(Collectors.toList());
        Map<Long, WmsPickTask> taskMap = pageTaskIds.isEmpty() ? Map.of()
                : taskMapper.selectBatchIds(pageTaskIds).stream()
                        .collect(Collectors.toMap(WmsPickTask::getId, Function.identity()));

        List<PickTaskDetailVO> voList = new ArrayList<>();
        for (WmsPickDetail item : itemPage.getRecords()) {
            PickTaskDetailVO vo = new PickTaskDetailVO();
            BeanUtils.copyProperties(item, vo);
            WmsPickTask task = taskMap.get(item.getTaskId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
                vo.setSourceType(task.getSourceType());
                vo.setSourceOrderNo(task.getSourceOrderNo());
                vo.setCustomerName(task.getCustomerName());
                vo.setWarehouseId(task.getWarehouseId());
                vo.setWarehouseName(task.getWarehouseName());
                vo.setTaskStatus(task.getStatus());
                vo.setTotalQuantity(task.getTotalQuantity());
                vo.setPickedTotalQuantity(task.getPickedQuantity());
                vo.setCreateTime(task.getCreateTime());
            }
            voList.add(vo);
        }
        Page<PickTaskDetailVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ══════════ 库存冻结（拣货只用 freeze，最终扣减由销售出库单 ship 统一执行） ══════════

    /** 拣货开始：冻结任务明细应拣量 */
    private void freezeTaskDetails(WmsPickTask task, Long operatorId, String operatorName) {
        List<WmsPickDetail> details = listByTaskId(task.getId());
        for (WmsPickDetail d : details) {
            if (d.getProductId() == null || d.getExpectedQuantity() == null || d.getExpectedQuantity().signum() <= 0) continue;
            inventoryService.freeze(d.getProductId(), task.getWarehouseId(), d.getLocationId(), d.getBatchNo(),
                    d.getExpectedQuantity(), "PICK-" + task.getId(), "WMS_PICK", task.getId(), operatorId, operatorName);
        }
    }

    /**
     * 拣货完成/作废结算：先释放全部冻结；deduct=true 时再按实拣量统一扣减（erp_stock 仅在此扣一次，禁双扣）。
     * 缺货行实拣量 = 应拣量 - 缺货量，扣减其已拣部分。
     */
    private void settleTaskDetails(WmsPickTask task, boolean deduct) {
        List<WmsPickDetail> details = listByTaskId(task.getId());
        for (WmsPickDetail d : details) {
            if (d.getProductId() == null || d.getExpectedQuantity() == null || d.getExpectedQuantity().signum() <= 0) continue;
            inventoryService.unfreeze(d.getProductId(), task.getWarehouseId(), d.getLocationId(), d.getBatchNo(),
                    d.getExpectedQuantity(), "PICK-" + task.getId(), "WMS_PICK", task.getId(), null, null);
            if (deduct) {
                BigDecimal picked = d.getPickedQuantity() != null ? d.getPickedQuantity() : BigDecimal.ZERO;
                if (picked.signum() > 0) {
                    inventoryService.decrease(d.getProductId(), task.getWarehouseId(), d.getLocationId(), d.getBatchNo(),
                            picked, "PICK-" + task.getId(), "WMS_PICK", task.getId(), task.getTaskNo(), null, null);
                }
            }
        }
    }
}
