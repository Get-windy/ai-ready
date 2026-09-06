package cn.aiedge.wms.receipt.service.impl;

import cn.aiedge.quality.service.QualityInspectionService;
import cn.aiedge.wms.controller.dto.WmsReceiptDetailVO;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.inventory.service.InventoryService;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.receipt.mapper.WmsReceiptTaskMapper;
import cn.aiedge.wms.receipt.mapper.WmsReceiptDetailMapper;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
public class ReceiptServiceImpl implements ReceiptService {

    private final WmsReceiptTaskMapper taskMapper;
    private final WmsReceiptDetailMapper detailMapper;
    private final InventoryService inventoryService;
    private final WarehouseService warehouseService;
    private final QualityInspectionService qualityInspectionService;

    /** WMS→ERP：收货确认回写采购订单已收数量（红线：WMS 只 HTTP 调用，不依赖 erp-purchase） */
    private static final String ERP_RECEIPT_BACKFILL_URL = "/api/erp/purchase/order/received";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${wms.erp.base-url:http://localhost:5655}")
    private String erpBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

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
            // 单号/来源单号关键字模糊（Controller 将 keyword 注入 taskNo 与 sourceOrderNo，or 匹配）
            boolean hasTaskNo = StringUtils.hasText(query.getTaskNo());
            boolean hasSourceOrderNo = StringUtils.hasText(query.getSourceOrderNo());
            if (hasTaskNo || hasSourceOrderNo) {
                wrapper.and(w -> {
                    if (hasTaskNo) w.like(WmsReceiptTask::getTaskNo, query.getTaskNo());
                    if (hasSourceOrderNo) w.like(WmsReceiptTask::getSourceOrderNo, query.getSourceOrderNo());
                });
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsReceiptTask::getSourceType, query.getSourceType());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsReceiptTask::getWarehouseId, query.getWarehouseId());
            }
            if (StringUtils.hasText(query.getWarehouseName())) {
                wrapper.like(WmsReceiptTask::getWarehouseName, query.getWarehouseName());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsReceiptTask::getStatus, query.getStatus());
            }
            if (query.getPriority() != null) {
                wrapper.eq(WmsReceiptTask::getPriority, query.getPriority());
            }
            if (query.getDateStart() != null) {
                wrapper.ge(WmsReceiptTask::getCreateTime, query.getDateStart().atStartOfDay());
            }
            if (query.getDateEnd() != null) {
                wrapper.le(WmsReceiptTask::getCreateTime, query.getDateEnd().atTime(java.time.LocalTime.MAX));
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
    public void saveDetails(Long taskId, List<WmsReceiptDetail> details) {
        WmsReceiptTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "收货");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        // 先删后插（逻辑删除旧明细）
        LambdaQueryWrapper<WmsReceiptDetail> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsReceiptDetail::getTaskId, taskId);
        detailMapper.delete(delWrapper);
        int lineNo = 1;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (WmsReceiptDetail detail : details) {
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
        log.info("收货明细保存: taskId={}, items={}, totalQuantity={}", taskId, details.size(), totalQuantity);
    }

    @Override
    public Page<WmsReceiptDetailVO> pageDetail(Page<WmsReceiptDetailVO> page, String keyword, String sourceOrderNo,
                                               Integer sourceType, Integer status, Long warehouseId,
                                               String warehouseName, String productName, String batchNo,
                                               java.time.LocalDate dateStart, java.time.LocalDate dateEnd) {
        // 1. 单据级过滤（取满足条件的单据ID集合）
        LambdaQueryWrapper<WmsReceiptTask> docWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            docWrapper.and(w -> w.like(WmsReceiptTask::getTaskNo, keyword)
                    .or().like(WmsReceiptTask::getSourceOrderNo, keyword));
        }
        if (StringUtils.hasText(sourceOrderNo)) {
            docWrapper.like(WmsReceiptTask::getSourceOrderNo, sourceOrderNo);
        }
        if (sourceType != null) {
            docWrapper.eq(WmsReceiptTask::getSourceType, sourceType);
        }
        if (status != null) {
            docWrapper.eq(WmsReceiptTask::getStatus, status);
        }
        if (warehouseId != null) {
            docWrapper.eq(WmsReceiptTask::getWarehouseId, warehouseId);
        }
        if (StringUtils.hasText(warehouseName)) {
            docWrapper.like(WmsReceiptTask::getWarehouseName, warehouseName);
        }
        if (dateStart != null) {
            docWrapper.ge(WmsReceiptTask::getCreateTime, dateStart.atStartOfDay());
        }
        if (dateEnd != null) {
            docWrapper.le(WmsReceiptTask::getCreateTime, dateEnd.atTime(java.time.LocalTime.MAX));
        }
        docWrapper.select(WmsReceiptTask::getId);
        List<WmsReceiptTask> docs = taskMapper.selectList(docWrapper);
        List<Long> docIds = docs.stream().map(WmsReceiptTask::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 2. 明细级过滤分页
        LambdaQueryWrapper<WmsReceiptDetail> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(WmsReceiptDetail::getTaskId, docIds);
        if (StringUtils.hasText(productName)) {
            itemWrapper.like(WmsReceiptDetail::getProductName, productName);
        }
        if (StringUtils.hasText(batchNo)) {
            itemWrapper.like(WmsReceiptDetail::getBatchNo, batchNo);
        }
        itemWrapper.orderByDesc(WmsReceiptDetail::getId);
        Page<WmsReceiptDetail> itemPage = detailMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(WmsReceiptDetail::getTaskId).distinct().collect(Collectors.toList());
        Map<Long, WmsReceiptTask> docMap = pageDocIds.isEmpty() ? Map.of() :
                taskMapper.selectBatchIds(pageDocIds).stream()
                        .collect(Collectors.toMap(WmsReceiptTask::getId, Function.identity()));

        List<WmsReceiptDetailVO> voList = new ArrayList<>();
        for (WmsReceiptDetail item : itemPage.getRecords()) {
            WmsReceiptDetailVO vo = new WmsReceiptDetailVO();
            BeanUtils.copyProperties(item, vo);
            WmsReceiptTask doc = docMap.get(item.getTaskId());
            if (doc != null) {
                vo.setTaskNo(doc.getTaskNo());
                vo.setSourceType(doc.getSourceType());
                vo.setSourceOrderNo(doc.getSourceOrderNo());
                vo.setWarehouseId(doc.getWarehouseId());
                vo.setWarehouseName(doc.getWarehouseName());
                vo.setSupplierId(doc.getSupplierId());
                vo.setSupplierName(doc.getSupplierName());
                vo.setDocStatus(doc.getStatus());
                vo.setTaskCreateTime(doc.getCreateTime());
            }
            voList.add(vo);
        }
        Page<WmsReceiptDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public String generateNo() {
        // RC + yyyyMMdd + 6位随机大写（对齐报损单 generateNo 模式；前端保存时仍以表单传入为准）
        return "RC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
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
        if (task.getWarehouseId() == null) {
            throw new WmsBusinessException("收货任务缺少仓库，无法入库");
        }
        // 未检不入库门禁(强制开启)：来源采购单质检未通过(PASS/CONCESSION)则拦截
        if (StringUtils.hasText(task.getSourceOrderNo())
                && !qualityInspectionService.hasPassed("PURCHASE_ORDER", task.getSourceOrderNo(), null)) {
            throw new WmsBusinessException("该来源采购单未质检通过，禁止收货入库，请先完成质检");
        }
        // 收货过账：逐明细增加可用库存（wms_inventory 货位/批次维度 + 镜像 erp_stock + 库存日志），同事务回滚
        List<WmsReceiptDetail> details = listByTaskId(taskId);
        for (WmsReceiptDetail d : details) {
            BigDecimal received = d.getReceivedQuantity() != null && d.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? d.getReceivedQuantity() : d.getExpectedQuantity();
            BigDecimal broken = d.getBrokenQuantity() != null ? d.getBrokenQuantity() : BigDecimal.ZERO;
            // 破损不入库：净入库 = 实收 - 破损；破损大于实收则非法
            if (broken.compareTo(received) > 0) {
                throw new WmsBusinessException(String.format("商品[%s]破损数量[%s]大于实收[%s]，请核对",
                        d.getProductName() == null ? d.getProductCode() : d.getProductName(), broken, received));
            }
            BigDecimal qty = received.subtract(broken);
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 批次/效期校验（FEFO 预留）：过期批次禁止收货入账
            if (d.getValidityDate() != null && d.getValidityDate().isBefore(LocalDateTime.now())) {
                throw new WmsBusinessException(String.format("商品[%s]批次[%s]有效期至[%s]已过期，禁止收货入库",
                        d.getProductName() == null ? d.getProductCode() : d.getProductName(),
                        d.getBatchNo() == null ? "无批次" : d.getBatchNo(),
                        d.getValidityDate().toLocalDate()));
            }
            Long locationId = d.getLocationId();
            if (locationId == null) {
                // 明细缺货位：取仓库推荐存储位首个作为默认收货暂存位
                List<WmsLocation> recs = warehouseService.recommendLocations(task.getWarehouseId(), d.getProductId(), qty);
                if (recs != null && !recs.isEmpty()) {
                    locationId = recs.get(0).getId();
                }
            }
            inventoryService.increase(d.getProductId(), task.getWarehouseId(), locationId, d.getBatchNo(),
                    qty, "RECEIPT-" + task.getTaskNo(), "RECEIPT",
                    task.getId(), task.getTaskNo(), userId, userName);
        }
        // 跨模块：收货确认后回写采购订单明细已收数量（WMS 只 HTTP 调用 ERP 接口）
        if (task.getSourceOrderId() != null) {
            callErpReceiveBackfill(task, details);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setCompletedTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("收货任务完成: taskId={}, userId={}, items={}", taskId, userId, details.size());
    }

    /** WMS→ERP：按 productId 聚合本次实收数量，HTTP 累加回写采购订单明细已收数 */
    private void callErpReceiveBackfill(WmsReceiptTask task, List<WmsReceiptDetail> details) {
        try {
            Map<Object, BigDecimal> byProduct = new HashMap<>();
            for (WmsReceiptDetail d : details) {
                if (d.getProductId() == null) continue;
                BigDecimal receivedQty = d.getReceivedQuantity() != null && d.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                        ? d.getReceivedQuantity() : d.getExpectedQuantity();
                BigDecimal brokenQty = d.getBrokenQuantity() != null ? d.getBrokenQuantity() : BigDecimal.ZERO;
                BigDecimal qty = receivedQty.subtract(brokenQty);
                if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) continue;
                byProduct.merge(d.getProductId(), qty, BigDecimal::add);
            }
            if (byProduct.isEmpty()) return;
            List<Map<String, Object>> items = byProduct.entrySet().stream()
                    .map(e -> Map.of("productId", e.getKey(), "receivedQuantity", e.getValue()))
                    .collect(Collectors.toList());
            String payload = objectMapper.writeValueAsString(Map.of(
                    "purchaseOrderId", task.getSourceOrderId(),
                    "items", items));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(erpBaseUrl + ERP_RECEIPT_BACKFILL_URL))
                    .header("Content-Type", "application/json")
                    .header("X-Trace-Id", "RECEIPT-" + task.getTaskNo())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new WmsBusinessException("调用ERP收货回写失败: HTTP " + response.statusCode() + " " + response.body());
            }
        } catch (WmsBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new WmsBusinessException("调用ERP收货回写异常: " + e.getMessage(), e);
        }
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
