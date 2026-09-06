package cn.aiedge.wms.putaway.service.impl;

import cn.aiedge.quality.service.QualityInspectionService;
import cn.aiedge.wms.controller.dto.WmsPutawayDetailQuery;
import cn.aiedge.wms.controller.dto.WmsPutawayDetailVO;
import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.entity.WmsPutawayDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.inventory.service.InventoryService;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.putaway.mapper.WmsPutawayTaskMapper;
import cn.aiedge.wms.putaway.mapper.WmsPutawayDetailMapper;
import cn.aiedge.wms.putaway.service.PutawayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayServiceImpl implements PutawayService {

    private final WmsPutawayTaskMapper taskMapper;
    private final WmsPutawayDetailMapper detailMapper;
    private final InventoryService inventoryService;
    private final QualityInspectionService qualityInspectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsPutawayTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsPutawayTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsPutawayTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsPutawayTask> pageTask(Page<WmsPutawayTask> page, WmsPutawayTask query) {
        LambdaQueryWrapper<WmsPutawayTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsPutawayTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsPutawayTask::getTaskNo, query.getTaskNo());
            }
            if (StringUtils.hasText(query.getSourceOrderNo())) {
                wrapper.like(WmsPutawayTask::getSourceOrderNo, query.getSourceOrderNo());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsPutawayTask::getSourceType, query.getSourceType());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsPutawayTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsPutawayTask::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsPutawayTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        WmsPutawayTask task = taskMapper.selectById(id);
        if (task == null) return false;
        int status = task.getStatus() == null ? -1 : task.getStatus();
        if (status != WmsTaskStatus.PENDING) {
            throw new WmsBusinessException(
                    String.format("任务[%s]当前状态[%d]不允许删除，仅待上架可删除", task.getTaskNo(), status));
        }
        // 级联删除明细
        LambdaQueryWrapper<WmsPutawayDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsPutawayDetail::getTaskId, id);
        detailMapper.delete(delWrapper);
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsPutawayDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsPutawayDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public List<WmsPutawayDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsPutawayDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsPutawayDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsPutawayDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetails(Long taskId, List<WmsPutawayDetail> details) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 先删后插（逻辑删除旧明细）
        LambdaQueryWrapper<WmsPutawayDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsPutawayDetail::getTaskId, taskId);
        detailMapper.delete(delWrapper);
        int lineNo = 1;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (WmsPutawayDetail detail : details) {
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
        log.info("上架明细保存: taskId={}, items={}, totalQuantity={}", taskId, details.size(), totalQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPutaway(Long taskId, Long userId, String userName) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 采购入库"未检不上架"门禁（PO/ASN 前缀白名单）：未通过质检禁止开始上架
        validateQcBeforeShelving(task.getSourceOrderNo());
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("上架任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPutaway(Long taskId, Long userId, String userName) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        // 明细
        List<WmsPutawayDetail> details = listByTaskId(taskId);
        if (details.isEmpty()) {
            throw new WmsBusinessException("上架明细为空，无法确认上架");
        }
        Long warehouseId = task.getWarehouseId();
        String taskNo = task.getTaskNo();
        if (warehouseId == null) {
            throw new WmsBusinessException("上架任务缺少仓库，无法确认上架");
        }
        BigDecimal putawayTotal = BigDecimal.ZERO;
        for (WmsPutawayDetail d : details) {
            BigDecimal qty = d.getQuantity() == null ? BigDecimal.ZERO : d.getQuantity();
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            // 批次效期到期校验（FEFO 前置）：有效期已过禁止上架
            if (d.getValidityDate() != null && d.getValidityDate().isBefore(LocalDateTime.now())) {
                throw new WmsBusinessException(String.format("商品[%s]批次[%s]有效期至[%s]已过期，禁止上架",
                        d.getProductName(), d.getBatchNo(), d.getValidityDate().toLocalDate()));
            }
            if (d.getProductId() == null) throw new WmsBusinessException("上架明细缺少商品，无法确认上架");
            if (d.getFromLocationId() == null || d.getToLocationId() == null) {
                throw new WmsBusinessException(String.format("商品[%s]明细缺少来源货位或目标货位，请补全后再确认上架",
                        d.getProductName()));
            }
            // 库位移动（红线：统一经 InventoryService.move，源/目标同减同增 + 库存日志，严禁绕过引擎）
            inventoryService.move(d.getProductId(), warehouseId, d.getFromLocationId(), d.getToLocationId(),
                    d.getBatchNo(), qty, "PUTAWAY", "PUTAWAY", taskId, userId, userName);
            putawayTotal = putawayTotal.add(qty);
        }
        // 明细全部置为已上架（status 0-待上架 → 1-已上架）
        LambdaQueryWrapper<WmsPutawayDetail> updWrapper = new LambdaQueryWrapper<>();
        updWrapper.eq(WmsPutawayDetail::getTaskId, taskId)
                .eq(WmsPutawayDetail::getStatus, WmsTaskStatus.PENDING);
        WmsPutawayDetail update = new WmsPutawayDetail();
        update.setStatus(1);
        detailMapper.update(update, updWrapper);
        // 反写已上架数量
        task.setPutawayQuantity(putawayTotal);
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("上架任务完成: taskId={}, userId={}, putawayQuantity={}", taskId, userId, putawayTotal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPutaway(Long taskId, String reason) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        int status = task.getStatus() == null ? -1 : task.getStatus();
        if (status != WmsTaskStatus.PENDING && status != WmsTaskStatus.IN_PROGRESS) {
            throw new WmsBusinessException(
                    String.format("任务[%s]当前状态[%d]不允许取消，仅待上架/上架中可取消", task.getTaskNo(), status));
        }
        task.setStatus(WmsTaskStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            String remark = task.getRemark();
            task.setRemark(remark == null || remark.isBlank() ? reason : remark + "；取消原因：" + reason);
        }
        taskMapper.updateById(task);
        log.info("上架任务取消: taskId={}, taskNo={}, reason={}", taskId, task.getTaskNo(), reason);
    }

    @Override
    public Page<WmsPutawayDetailVO> pageDetail(Page<WmsPutawayDetail> page, WmsPutawayDetailQuery query) {
        // 1. 先取满足单据级过滤的任务ID集合
        LambdaQueryWrapper<WmsPutawayTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.select(WmsPutawayTask::getId);
        if (StringUtils.hasText(query.getTaskNo())) taskWrapper.like(WmsPutawayTask::getTaskNo, query.getTaskNo());
        if (StringUtils.hasText(query.getSourceOrderNo())) taskWrapper.like(WmsPutawayTask::getSourceOrderNo, query.getSourceOrderNo());
        if (query.getSourceType() != null) taskWrapper.eq(WmsPutawayTask::getSourceType, query.getSourceType());
        if (query.getStatus() != null) taskWrapper.eq(WmsPutawayTask::getStatus, query.getStatus());
        if (query.getWarehouseId() != null) taskWrapper.eq(WmsPutawayTask::getWarehouseId, query.getWarehouseId());
        if (StringUtils.hasText(query.getAssigneeName())) taskWrapper.like(WmsPutawayTask::getAssigneeName, query.getAssigneeName());
        if (StringUtils.hasText(query.getRemark())) taskWrapper.like(WmsPutawayTask::getRemark, query.getRemark());
        List<Long> taskIds = taskMapper.selectList(taskWrapper).stream()
                .map(WmsPutawayTask::getId).collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }
        // 2. 明细过滤
        LambdaQueryWrapper<WmsPutawayDetail> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(WmsPutawayDetail::getTaskId, taskIds);
        if (StringUtils.hasText(query.getProductName())) itemWrapper.like(WmsPutawayDetail::getProductName, query.getProductName());
        if (StringUtils.hasText(query.getProductCode())) itemWrapper.like(WmsPutawayDetail::getProductCode, query.getProductCode());
        if (StringUtils.hasText(query.getLocationCode())) {
            itemWrapper.and(w -> w.like(WmsPutawayDetail::getFromLocationCode, query.getLocationCode())
                    .or().like(WmsPutawayDetail::getToLocationCode, query.getLocationCode()));
        }
        itemWrapper.orderByDesc(WmsPutawayDetail::getId);
        Page<WmsPutawayDetail> itemPage = detailMapper.selectPage(page, itemWrapper);
        // 3. 批量补齐任务主表字段
        List<Long> pageTaskIds = itemPage.getRecords().stream()
                .map(WmsPutawayDetail::getTaskId).distinct().collect(Collectors.toList());
        Map<Long, WmsPutawayTask> taskMap = pageTaskIds.isEmpty() ? Map.of() :
                taskMapper.selectBatchIds(pageTaskIds).stream()
                        .collect(Collectors.toMap(WmsPutawayTask::getId, Function.identity()));
        List<WmsPutawayDetailVO> voList = new ArrayList<>();
        for (WmsPutawayDetail d : itemPage.getRecords()) {
            WmsPutawayDetailVO vo = new WmsPutawayDetailVO();
            BeanUtils.copyProperties(d, vo);
            WmsPutawayTask task = taskMap.get(d.getTaskId());
            if (task != null) {
                vo.setTaskNo(task.getTaskNo());
                vo.setSourceType(task.getSourceType());
                vo.setSourceId(task.getSourceId());
                vo.setSourceOrderNo(task.getSourceOrderNo());
                vo.setWarehouseId(task.getWarehouseId());
                vo.setWarehouseName(task.getWarehouseName());
                vo.setTotalQuantity(task.getTotalQuantity());
                vo.setPutawayQuantity(task.getPutawayQuantity());
                vo.setAssigneeId(task.getAssigneeId());
                vo.setAssigneeName(task.getAssigneeName());
                vo.setTaskStatus(task.getStatus());
                vo.setTaskCreateTime(task.getCreateTime());
            }
            voList.add(vo);
        }
        Page<WmsPutawayDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 采购入库"未检不上架"门禁（硬编码白名单）：
     * sourceOrderNo 前缀为 PO/ASN（采购单/到货通知）时强制校验关联质检，非 PASS 阻断上架；
     * 其它前缀（退货/调拨/其他等）无条件放行，避免误阻内部流转单。
     */
    private void validateQcBeforeShelving(String sourceOrderNo) {
        if (sourceOrderNo == null || sourceOrderNo.isBlank()) return;
        if (sourceOrderNo.startsWith("PO") || sourceOrderNo.startsWith("ASN")) {
            if (!qualityInspectionService.hasPassed("PURCHASE_ORDER", sourceOrderNo, null)) {
                throw new WmsBusinessException(String.format("采购单[%s]关联货品未通过质检，禁止上架", sourceOrderNo));
            }
        }
    }
}
