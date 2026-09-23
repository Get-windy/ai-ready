package cn.aiedge.wms.ship.service.impl;

import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.inventory.service.InventoryService;
import cn.aiedge.wms.ship.mapper.WmsShipTaskMapper;
import cn.aiedge.wms.ship.mapper.WmsShipDetailMapper;
import cn.aiedge.wms.ship.service.ShipService;
import cn.aiedge.wms.ship.dto.ShipQuery;
import cn.aiedge.wms.ship.dto.WmsShipDetailPageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipServiceImpl implements ShipService {

    private static final String SRC_SHIP = "WMS_SHIP";

    private final WmsShipTaskMapper taskMapper;
    private final WmsShipDetailMapper detailMapper;
    private final InventoryService inventoryService;

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
            // 单号/来源单号关键字模糊（Controller 将 keyword 注入 taskNo 与 sourceOrderNo，or 匹配）
            boolean hasTaskNo = StringUtils.hasText(query.getTaskNo());
            boolean hasSourceOrderNo = StringUtils.hasText(query.getSourceOrderNo());
            if (hasTaskNo || hasSourceOrderNo) {
                wrapper.and(w -> {
                    if (hasTaskNo) w.like(WmsShipTask::getTaskNo, query.getTaskNo());
                    if (hasSourceOrderNo) w.like(WmsShipTask::getSourceOrderNo, query.getSourceOrderNo());
                });
            }
            if (StringUtils.hasText(query.getCarrierName())) {
                wrapper.like(WmsShipTask::getCarrierName, query.getCarrierName());
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
    public Page<WmsShipTask> queryPage(Page<WmsShipTask> page, ShipQuery query) {
        LambdaQueryWrapper<WmsShipTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            boolean kw = StringUtils.hasText(query.getKeyword());
            if (kw || StringUtils.hasText(query.getSourceOrderNo())) {
                wrapper.and(w -> {
                    if (kw) {
                        w.like(WmsShipTask::getTaskNo, query.getKeyword())
                                .or().like(WmsShipTask::getSourceOrderNo, query.getKeyword());
                    }
                    if (StringUtils.hasText(query.getSourceOrderNo())) {
                        w.like(WmsShipTask::getSourceOrderNo, query.getSourceOrderNo());
                    }
                });
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsShipTask::getWarehouseId, query.getWarehouseId());
            }
            if (StringUtils.hasText(query.getWarehouseName())) {
                wrapper.like(WmsShipTask::getWarehouseName, query.getWarehouseName());
            }
            if (StringUtils.hasText(query.getCustomerName())) {
                wrapper.like(WmsShipTask::getCustomerName, query.getCustomerName());
            }
            if (StringUtils.hasText(query.getCarrierName())) {
                wrapper.like(WmsShipTask::getCarrierName, query.getCarrierName());
            }
            if (StringUtils.hasText(query.getTrackingNo())) {
                wrapper.like(WmsShipTask::getTrackingNo, query.getTrackingNo());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsShipTask::getStatus, query.getStatus());
            }
            if (StringUtils.hasText(query.getStartDate())) {
                wrapper.ge(WmsShipTask::getCreateTime, query.getStartDate());
            }
            if (StringUtils.hasText(query.getEndDate())) {
                wrapper.le(WmsShipTask::getCreateTime, query.getEndDate() + " 23:59:59");
            }
        }
        wrapper.orderByDesc(WmsShipTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<WmsShipDetailPageVO> pageDetail(IPage<WmsShipDetailPageVO> page, ShipQuery query) {
        return detailMapper.selectDetailPage(page, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetails(Long shipId, List<WmsShipDetail> details) {
        WmsShipTask task = taskMapper.selectById(shipId);
        if (task == null) throw WmsBusinessException.taskNotFound(shipId, "发货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 先删后插（逻辑删除旧明细）
        LambdaQueryWrapper<WmsShipDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsShipDetail::getShipId, shipId);
        detailMapper.delete(delWrapper);
        int lineNo = 1;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (WmsShipDetail detail : details) {
            detail.setId(null);
            detail.setShipId(shipId);
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
        log.info("发货明细保存: shipId={}, items={}, totalQuantity={}", shipId, details.size(), totalQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startShip(Long taskId, Long userId, String userName) {
        WmsShipTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "发货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
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
        if (detail == null) throw new WmsBusinessException("发货明细[" + detailId + "]不存在");
        if (detail.getStatus() != 0) {
            throw new WmsBusinessException("发货明细[" + detailId + "]当前状态不允许扫描，仅待扫描可扫");
        }
        detail.setScannedQuantity(scannedQuantity);
        detail.setStatus(1); // 已扫描
        detailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShip(Long taskId) {
        WmsShipTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "发货");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        List<WmsShipDetail> details = listByShipId(taskId);
        if (details.isEmpty()) {
            throw new WmsBusinessException("发货单[" + task.getTaskNo() + "]无发货明细，不能确认发货");
        }
        Long operatorId = task.getAssigneeId() != null ? task.getAssigneeId() : 0L;
        String operatorName = task.getAssigneeName() != null ? task.getAssigneeName() : "系统";
        String traceId = UUID.randomUUID().toString();
        // P2 强制校验：确认数量 > 0 且批次库存可用量足够（严禁超发）
        for (WmsShipDetail d : details) {
            BigDecimal qty = d.getConfirmedQuantity() != null ? d.getConfirmedQuantity() : d.getScannedQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new WmsBusinessException("商品[" + d.getProductName() + "]确认数量必须大于0");
            }
            String batch = d.getBatchNo() != null ? d.getBatchNo() : "无批次";
            WmsInventory inv = inventoryService.getByUniqueKey(d.getProductId(), task.getWarehouseId(), d.getLocationId(), d.getBatchNo());
            if (inv == null) {
                throw new WmsBusinessException("商品[" + d.getProductName() + "]批次[" + batch + "]库存不存在");
            }
            if (inv.getAvailableQuantity() == null || inv.getAvailableQuantity().compareTo(qty) < 0) {
                throw new WmsBusinessException("商品[" + d.getProductName() + "]批次[" + batch + "]可用库存["
                        + (inv.getAvailableQuantity() == null ? 0 : inv.getAvailableQuantity()) + "]不足");
            }
        }
        // 唯一扣减入口：InventoryService.decrease 在同一事务内 双写 wms_inventory + 镜像 ERP 轨 erp_stock（强一致）。
        // 双轨已由该单入口统一，故无需事件总线（事件总线会让 ERP 再 decreaseStock 导致重复扣 erp_stock）；
        // updateStock(erp-sales) 的第二个扣减链路已屏蔽。此处严禁再触达 erp_stock 或 EventPublisher。
        for (WmsShipDetail d : details) {
            BigDecimal qty = d.getConfirmedQuantity() != null ? d.getConfirmedQuantity() : d.getScannedQuantity();
            inventoryService.decrease(d.getProductId(), task.getWarehouseId(), d.getLocationId(), d.getBatchNo(),
                    qty, traceId, SRC_SHIP, taskId, task.getTaskNo(), operatorId, operatorName);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setShipTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("发货确认完成(本地冻结): taskId={}", taskId);
    }

    /**
     * 取消发货任务（待复核 0 / 复核中 1 可取消）。
     *
     * <p>库存侧无需回滚：出库扣减的唯一入口是 {@link #confirmShip(Long)}，
     * 未确认发货前库存未发生任何变化（本单不做前置冻结，冻结发生在拣货单侧）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelShip(Long taskId, String reason) {
        WmsShipTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "发货");
        if (task.getStatus() != WmsTaskStatus.PENDING
                && task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.CANCELLED);
        task.setRemark(reason);
        taskMapper.updateById(task);
        log.info("发货任务取消: taskId={}, reason={}", taskId, reason);
    }
}
