package cn.aiedge.wms.borrow.service.impl;

import cn.aiedge.wms.borrow.dto.BorrowOrderItemVO;
import cn.aiedge.wms.borrow.dto.BorrowOrderQuery;
import cn.aiedge.wms.borrow.dto.BorrowReturnRequest;
import cn.aiedge.wms.borrow.dto.ConvertPurchaseRequest;
import cn.aiedge.wms.borrow.dto.WmsBorrowOrderVO;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderItemMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowReturnItemMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowReturnMapper;
import cn.aiedge.wms.borrow.service.BorrowService;
import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowOrderItem;
import cn.aiedge.wms.entity.WmsBorrowReturn;
import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.inventory.mapper.WmsInventoryMapper;
import cn.aiedge.wms.inventory.service.InventoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    /** 库存异动来源类型 */
    private static final String SRC_BORROW_IN = "WMS_BORROW_IN";
    private static final String SRC_BORROW_OUT = "WMS_BORROW_OUT";
    private static final String SRC_BORROW_IN_RETURN = "WMS_BORROW_IN_RETURN";
    private static final String SRC_BORROW_OUT_RETURN = "WMS_BORROW_OUT_RETURN";

    private final WmsBorrowOrderMapper orderMapper;
    private final WmsBorrowOrderItemMapper itemMapper;
    private final WmsBorrowReturnMapper returnMapper;
    private final WmsBorrowReturnItemMapper returnItemMapper;
    private final InventoryService inventoryService;
    private final WmsInventoryMapper inventoryMapper;

    @Override
    public Page<WmsBorrowOrder> pageOrder(Page<WmsBorrowOrder> page, WmsBorrowOrder query) {
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsBorrowOrder::getId, query.getId());
            }
            if (query.getOrderNo() != null) {
                wrapper.like(WmsBorrowOrder::getOrderNo, query.getOrderNo());
            }
            if (query.getDirection() != null) {
                wrapper.eq(WmsBorrowOrder::getDirection, query.getDirection());
            }
            if (query.getPartnerId() != null) {
                wrapper.eq(WmsBorrowOrder::getPartnerId, query.getPartnerId());
            }
            if (query.getPartnerName() != null) {
                wrapper.like(WmsBorrowOrder::getPartnerName, query.getPartnerName());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsBorrowOrder::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsBorrowOrder::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsBorrowOrder::getId);
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    public WmsBorrowOrderVO getOrderDetail(Long id) {
        WmsBorrowOrder order = orderMapper.selectById(id);
        if (order == null) return null;
        WmsBorrowOrderVO vo = new WmsBorrowOrderVO();
        org.springframework.beans.BeanUtils.copyProperties(order, vo);
        vo.setItems(listItems(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsBorrowOrder createOrder(WmsBorrowOrderVO order) {
        validateDirection(order.getDirection());
        order.setId(null);
        order.setOrderNo(generateOrderNo(order.getDirection()));
        order.setStatus(STATUS_DRAFT);
        order.setReturnedQuantity(BigDecimal.ZERO);
        fillTotals(order, order.getItems());
        orderMapper.insert(order);
        saveItems(order.getId(), order.getItems());
        log.info("新建借进借出单: id={}, orderNo={}, direction={}", order.getId(), order.getOrderNo(), order.getDirection());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(WmsBorrowOrderVO order) {
        WmsBorrowOrder existing = orderMapper.selectById(order.getId());
        if (existing == null) throw new WmsBusinessException("借进借出单不存在: " + order.getId());
        if (existing.getStatus() != STATUS_DRAFT) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿状态可修改", existing.getOrderNo()));
        }
        validateDirection(order.getDirection());
        // 单号/状态/已归还数量不允许改
        order.setOrderNo(existing.getOrderNo());
        order.setStatus(STATUS_DRAFT);
        order.setReturnedQuantity(existing.getReturnedQuantity());
        fillTotals(order, order.getItems());
        orderMapper.updateById(order);
        // 明细整体替换（先删后插）
        LambdaQueryWrapper<WmsBorrowOrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsBorrowOrderItem::getOrderId, existing.getId());
        itemMapper.delete(delWrapper);
        saveItems(existing.getId(), order.getItems());
        log.info("更新借进借出单: id={}, orderNo={}", existing.getId(), existing.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrder(Long id) {
        WmsBorrowOrder existing = orderMapper.selectById(id);
        if (existing == null) throw new WmsBusinessException("借进借出单不存在: " + id);
        if (existing.getStatus() != STATUS_DRAFT && existing.getStatus() != STATUS_CANCELLED) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿/已取消状态可删除", existing.getOrderNo()));
        }
        orderMapper.deleteById(id);
        LambdaQueryWrapper<WmsBorrowOrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(WmsBorrowOrderItem::getOrderId, id);
        itemMapper.delete(delWrapper);
        log.info("删除借进借出单: id={}, orderNo={}", id, existing.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_DRAFT) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿状态可提交审批", order.getOrderNo()));
        }
        order.setStatus(STATUS_PENDING_APPROVAL);
        orderMapper.updateById(order);
        log.info("提交审批: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long operatorId, String operatorName) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_PENDING_APPROVAL) {
            throw new WmsBusinessException(String.format("单据[%s]仅待审批状态可审批", order.getOrderNo()));
        }
        applyStockAndMarkApproved(order, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void post(Long id, Long operatorId, String operatorName) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_DRAFT && order.getStatus() != STATUS_PENDING_APPROVAL) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿/待审批状态可记账", order.getOrderNo()));
        }
        applyStockAndMarkApproved(order, operatorId, operatorName);
    }

    /** 记账入库/出库：借进库存增加、借出库存扣减，状态置为已审批（已记账） */
    private void applyStockAndMarkApproved(WmsBorrowOrder order, Long operatorId, String operatorName) {
        Long id = order.getId();
        List<WmsBorrowOrderItem> items = listItems(id);
        if (items.isEmpty()) {
            throw new WmsBusinessException(String.format("单据[%s]无明细，不能记账", order.getOrderNo()));
        }
        String traceId = UUID.randomUUID().toString();
        boolean borrowIn = order.getDirection() == DIRECTION_IN;
        for (WmsBorrowOrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            if (borrowIn) {
                // 借进：库存增加
                ensureInventoryRow(item, order);
                inventoryService.increase(item.getProductId(), order.getWarehouseId(), null, null,
                        item.getQuantity(), traceId, SRC_BORROW_IN, order.getId(), order.getOrderNo(),
                        operatorId, operatorName);
            } else {
                // 借出：库存按批次扣减（批次为空则整仓扣减）；decrease 内部校验可用量不足
                inventoryService.decrease(item.getProductId(), order.getWarehouseId(), null, item.getBatchCode(),
                        item.getQuantity(), traceId, SRC_BORROW_OUT, order.getId(), order.getOrderNo(),
                        operatorId, operatorName);
            }
        }
        order.setStatus(STATUS_APPROVED);
        order.setBookkeeperId(operatorId);
        order.setBookkeeperName(operatorName);
        order.setBookkeepingTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("记账/审批: id={}, orderNo={}, direction={}, traceId={}", id, order.getOrderNo(), order.getDirection(), traceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WmsBorrowOrder order = getOrThrow(id);
        if (order.getStatus() != STATUS_DRAFT && order.getStatus() != STATUS_PENDING_APPROVAL) {
            throw new WmsBusinessException(String.format("单据[%s]仅草稿/待审批状态可取消", order.getOrderNo()));
        }
        order.setStatus(STATUS_CANCELLED);
        orderMapper.updateById(order);
        log.info("取消单据: id={}, orderNo={}", id, order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> aggregateByProduct(Integer direction, String partnerName,
                                                        String productName, String dateStart, String dateEnd,
                                                        Long categoryId, String handlerName, String deptName) {
        // 空串归一为 null，避免 SQL 中 CAST('' AS DATE) 报错
        String start = StringUtils.hasText(dateStart) ? dateStart : null;
        String end = StringUtils.hasText(dateEnd) ? dateEnd : null;
        String handler = StringUtils.hasText(handlerName) ? handlerName : null;
        String dept = StringUtils.hasText(deptName) ? deptName : null;
        List<Map<String, Object>> raw = itemMapper.aggregateByProduct(direction, partnerName, productName, start, end, categoryId, handler, dept);
        // PostgreSQL 不加引号别名会转小写，统一转驼峰供前端使用
        List<Map<String, Object>> result = new java.util.ArrayList<>(raw.size());
        for (Map<String, Object> row : raw) {
            Map<String, Object> camel = new java.util.LinkedHashMap<>(row.size());
            for (Map.Entry<String, Object> e : row.entrySet()) {
                camel.put(toCamelCase(e.getKey()), e.getValue());
            }
            result.add(camel);
        }
        return result;
    }

    private String toCamelCase(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public WmsBorrowReturn returnOrder(BorrowReturnRequest request) {
        WmsBorrowOrder order = getOrThrow(request.getOrderId());
        if (order.getStatus() != STATUS_APPROVED && order.getStatus() != STATUS_PARTIAL_RETURNED) {
            throw new WmsBusinessException(String.format("单据[%s]当前状态不允许归还", order.getOrderNo()));
        }
        boolean borrowIn = order.getDirection() == DIRECTION_IN;
        String traceId = UUID.randomUUID().toString();
        BigDecimal orderReturned = nvl(order.getReturnedQuantity());

        // 归还记录头
        WmsBorrowReturn ret = new WmsBorrowReturn();
        ret.setOrderId(order.getId());
        ret.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now());
        ret.setOperatorId(request.getOperatorId());
        ret.setOperatorName(request.getOperatorName());
        ret.setRemark(request.getRemark());
        returnMapper.insert(ret);

        for (WmsBorrowReturnItem ri : request.getItems()) {
            if (ri.getQuantity() == null || ri.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new WmsBusinessException("归还数量必须大于0");
            }
            WmsBorrowOrderItem item = itemMapper.selectById(ri.getOrderItemId());
            if (item == null || !item.getOrderId().equals(order.getId())) {
                throw new WmsBusinessException("归还明细不属于本单据: orderItemId=" + ri.getOrderItemId());
            }
            BigDecimal remaining = nvl(item.getQuantity()).subtract(nvl(item.getReturnedQuantity()));
            if (ri.getQuantity().compareTo(remaining) > 0) {
                throw new WmsBusinessException(String.format("商品[%s]归还数量超出未归还数量: 本次=%s, 剩余=%s",
                        item.getProductCode(), ri.getQuantity(), remaining));
            }
            // 累计明细已归还数量（UpdateWrapper 更新，绕开乐观锁，明细表无 version 列）
            LambdaUpdateWrapper<WmsBorrowOrderItem> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WmsBorrowOrderItem::getId, item.getId())
                    .set(WmsBorrowOrderItem::getReturnedQuantity, nvl(item.getReturnedQuantity()).add(ri.getQuantity()));
            itemMapper.update(null, updateWrapper);

            // 库存反向回冲：借进归还=库存扣减（还给对方），借出归还=库存增加（收回）
            if (borrowIn) {
                inventoryService.decrease(item.getProductId(), order.getWarehouseId(), null, null,
                        ri.getQuantity(), traceId, SRC_BORROW_IN_RETURN, ret.getId(), order.getOrderNo(),
                        request.getOperatorId(), request.getOperatorName());
            } else {
                ensureInventoryRow(item, order);
                inventoryService.increase(item.getProductId(), order.getWarehouseId(), null, null,
                        ri.getQuantity(), traceId, SRC_BORROW_OUT_RETURN, ret.getId(), order.getOrderNo(),
                        request.getOperatorId(), request.getOperatorName());
            }

            // 归还记录明细
            ri.setId(null);
            ri.setReturnId(ret.getId());
            ri.setProductId(item.getProductId());
            ri.setProductCode(item.getProductCode());
            ri.setProductName(item.getProductName());
            returnItemMapper.insert(ri);

            orderReturned = orderReturned.add(ri.getQuantity());
        }

        // 回写头表累计与状态
        order.setReturnedQuantity(orderReturned);
        boolean fullyReturned = orderReturned.compareTo(nvl(order.getTotalQuantity())) >= 0;
        order.setStatus(fullyReturned ? STATUS_RETURNED : STATUS_PARTIAL_RETURNED);
        orderMapper.updateById(order);
        log.info("归还登记: orderId={}, orderNo={}, returnId={}, returned={}, status={}",
                order.getId(), order.getOrderNo(), ret.getId(), orderReturned, order.getStatus());
        return ret;
    }

    @Override
    public Page<WmsBorrowReturn> pageReturn(Page<WmsBorrowReturn> page, Long orderId) {
        LambdaQueryWrapper<WmsBorrowReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orderId != null, WmsBorrowReturn::getOrderId, orderId);
        wrapper.orderByDesc(WmsBorrowReturn::getId);
        return returnMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WmsBorrowReturnItem> listReturnItems(Long returnId) {
        LambdaQueryWrapper<WmsBorrowReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsBorrowReturnItem::getReturnId, returnId);
        return returnItemMapper.selectList(wrapper);
    }

    @Override
    public String generateOrderNo(Integer direction) {
        String prefix = (direction != null && direction == DIRECTION_OUT) ? "JCD" : "JJD";
        String head = prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(WmsBorrowOrder::getOrderNo, head);
        wrapper.orderByDesc(WmsBorrowOrder::getOrderNo);
        wrapper.last("LIMIT 1");
        WmsBorrowOrder last = orderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null) {
            seq = Integer.parseInt(last.getOrderNo().substring(head.length())) + 1;
        }
        return head + String.format("%03d", seq);
    }

    @Override
    public String generateNo(Integer direction) {
        return generateOrderNo(direction != null ? direction : DIRECTION_IN);
    }

    @Override
    public Page<WmsBorrowOrder> pageOrderByQuery(BorrowOrderQuery query) {
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = buildDocWrapper(query);
        wrapper.orderByDesc(WmsBorrowOrder::getBorrowDate).orderByDesc(WmsBorrowOrder::getId);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    @Override
    public Page<BorrowOrderItemVO> pageDetail(BorrowOrderQuery query) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<WmsBorrowOrder> docWrapper = buildDocWrapper(query);
        docWrapper.select(WmsBorrowOrder::getId);
        List<WmsBorrowOrder> docs = orderMapper.selectList(docWrapper);
        List<Long> docIds = docs.stream().map(WmsBorrowOrder::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<WmsBorrowOrderItem> itemWrapper = new LambdaQueryWrapper<WmsBorrowOrderItem>()
                .in(WmsBorrowOrderItem::getOrderId, docIds)
                .like(StringUtils.hasText(query.getProductName()), WmsBorrowOrderItem::getProductName, query.getProductName())
                .like(StringUtils.hasText(query.getItemRemark()), WmsBorrowOrderItem::getRemark, query.getItemRemark())
                .orderByAsc(WmsBorrowOrderItem::getLineNo);
        Page<WmsBorrowOrderItem> itemPage = itemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(WmsBorrowOrderItem::getOrderId).distinct().collect(Collectors.toList());
        Map<Long, WmsBorrowOrder> docMap = pageDocIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectBatchIds(pageDocIds).stream()
                .collect(Collectors.toMap(WmsBorrowOrder::getId, Function.identity()));

        // 4. 联查往来单位主数据（客户级别/联系人/客户备注）
        Set<Long> partnerIds = docMap.values().stream()
                .map(WmsBorrowOrder::getPartnerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Map<String, Object>> partyMap = partnerIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectPartiesByIds(new ArrayList<>(partnerIds)).stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("id")).longValue(), Function.identity(), (a, b) -> a));
        Map<Long, String> contactMap = partnerIds.isEmpty() ? Collections.emptyMap()
                : orderMapper.selectContactsByIds(new ArrayList<>(partnerIds)).stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("partyId")).longValue(),
                        m -> (String) m.get("contact"), (a, b) -> a));

        List<BorrowOrderItemVO> voList = new ArrayList<>();
        for (WmsBorrowOrderItem item : itemPage.getRecords()) {
            BorrowOrderItemVO vo = new BorrowOrderItemVO();
            org.springframework.beans.BeanUtils.copyProperties(item, vo);
            WmsBorrowOrder doc = docMap.get(item.getOrderId());
            if (doc != null) {
                vo.setBorrowDate(doc.getBorrowDate());
                vo.setOrderNo(doc.getOrderNo());
                vo.setStatus(doc.getStatus());
                vo.setWarehouseId(doc.getWarehouseId());
                vo.setWarehouseName(doc.getWarehouseName());
                vo.setPartnerId(doc.getPartnerId());
                vo.setPartnerCode(doc.getPartnerCode());
                vo.setPartnerName(doc.getPartnerName());
                vo.setHandlerName(doc.getHandlerName());
                vo.setDeptName(doc.getDeptName());
                vo.setDocRemark(doc.getRemark());
                vo.setSummary(doc.getSummary());
                vo.setAttachment(doc.getAttachment());
                vo.setBookkeeperName(doc.getBookkeeperName());
                vo.setCreatorName(doc.getCreatorName());
                vo.setBookkeepingTime(doc.getBookkeepingTime());
                vo.setCreateTime(doc.getCreateTime());
                vo.setPrintCount(doc.getPrintCount());
                vo.setExpectedReturnDate(doc.getExpectedReturnDate());
                vo.setTotalWeight(doc.getTotalWeight());
                vo.setTotalVolume(doc.getTotalVolume());
                // 往来单位主数据穿透（客户级别/客户备注/主要联系人）
                if (doc.getPartnerId() != null) {
                    Map<String, Object> party = partyMap.get(doc.getPartnerId());
                    if (party != null) {
                        vo.setCustomerLevel((String) party.get("partyLevel"));
                        vo.setCustomerRemark((String) party.get("customerRemark"));
                    }
                    vo.setContact(contactMap.get(doc.getPartnerId()));
                }
            }
            // 未处理数量/金额（快照外计算，防止历史数据为空）
            BigDecimal qty = nvl(item.getQuantity());
            BigDecimal retd = nvl(item.getReturnedQuantity());
            BigDecimal conv = nvl(item.getProcessedPurchaseQuantity());
            BigDecimal nonProcessed = nvl(item.getNonProcessedQuantity());
            if (nonProcessed.compareTo(BigDecimal.ZERO) == 0) {
                nonProcessed = qty.subtract(retd).subtract(conv).max(BigDecimal.ZERO);
            }
            vo.setProcessedReturnQuantity(retd);
            vo.setProcessedPurchaseQuantity(conv);
            vo.setNonProcessedQuantity(nonProcessed);
            vo.setNonProcessedAmount(nvl(item.getNonProcessedAmount()).compareTo(BigDecimal.ZERO) == 0
                    ? nonProcessed.multiply(nvl(item.getPrice())) : item.getNonProcessedAmount());
            voList.add(vo);
        }
        Page<BorrowOrderItemVO> voPage = new Page<>(query.getPageNum(), query.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsBorrowOrder convertPurchase(ConvertPurchaseRequest request) {
        WmsBorrowOrder order = getOrThrow(request.getOrderId());
        return applyConvert(order, request.getItems(), "借转采购");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsBorrowOrder convertSale(ConvertPurchaseRequest request) {
        WmsBorrowOrder order = getOrThrow(request.getOrderId());
        return applyConvert(order, request.getItems(), "借转销售");
    }

    /** 借进借出「借转」台账登记共用逻辑（借进=借转采购，借出=借转销售）：更新明细已处理-借转数量/未处理数量，单据借转金额/数量 */
    private WmsBorrowOrder applyConvert(WmsBorrowOrder order, List<ConvertPurchaseRequest.Item> items, String actionLabel) {
        if (order.getStatus() != STATUS_APPROVED && order.getStatus() != STATUS_PARTIAL_RETURNED) {
            throw new WmsBusinessException(String.format("单据[%s]仅已记账状态可%s", order.getOrderNo(), actionLabel));
        }
        if (items == null || items.isEmpty()) {
            throw new WmsBusinessException("请填写" + actionLabel + "明细");
        }
        BigDecimal orderConvertQty = nvl(order.getConvertPurchaseQuantity());
        BigDecimal orderConvertAmt = nvl(order.getConvertPurchaseAmount());
        for (ConvertPurchaseRequest.Item ci : items) {
            if (ci.getQuantity() == null || ci.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new WmsBusinessException(actionLabel + "数量必须大于0");
            }
            WmsBorrowOrderItem item = itemMapper.selectById(ci.getOrderItemId());
            if (item == null || !item.getOrderId().equals(order.getId())) {
                throw new WmsBusinessException(actionLabel + "明细不属于本单据: orderItemId=" + ci.getOrderItemId());
            }
            BigDecimal qty = nvl(item.getQuantity());
            BigDecimal returned = nvl(item.getReturnedQuantity());
            BigDecimal processed = nvl(item.getProcessedPurchaseQuantity());
            BigDecimal remaining = qty.subtract(returned).subtract(processed).max(BigDecimal.ZERO);
            if (ci.getQuantity().compareTo(remaining) > 0) {
                throw new WmsBusinessException(String.format("商品[%s]%s数量超出剩余: 本次=%s, 剩余=%s",
                        item.getProductCode(), actionLabel, ci.getQuantity(), remaining));
            }
            BigDecimal newProcessed = processed.add(ci.getQuantity());
            BigDecimal newNonProcessed = qty.subtract(returned).subtract(newProcessed).max(BigDecimal.ZERO);
            // 更新明细（绕开乐观锁，明细表无 version 列）
            LambdaUpdateWrapper<WmsBorrowOrderItem> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WmsBorrowOrderItem::getId, item.getId())
                    .set(WmsBorrowOrderItem::getProcessedPurchaseQuantity, newProcessed)
                    .set(WmsBorrowOrderItem::getNonProcessedQuantity, newNonProcessed)
                    .set(WmsBorrowOrderItem::getNonProcessedAmount, newNonProcessed.multiply(nvl(item.getPrice())));
            itemMapper.update(null, updateWrapper);

            orderConvertQty = orderConvertQty.add(ci.getQuantity());
            orderConvertAmt = orderConvertAmt.add(ci.getQuantity().multiply(nvl(item.getPrice())));
        }
        order.setConvertPurchaseQuantity(orderConvertQty);
        order.setConvertPurchaseAmount(orderConvertAmt);
        BigDecimal docNonProcessed = nvl(order.getTotalQuantity())
                .subtract(nvl(order.getReturnedQuantity())).subtract(orderConvertQty).max(BigDecimal.ZERO);
        order.setNonProcessedQuantity(docNonProcessed);
        order.setNonProcessedAmount(docNonProcessed.multiply(
                orderConvertQty.compareTo(BigDecimal.ZERO) > 0 ? orderConvertAmt.divide(orderConvertQty, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO));
        orderMapper.updateById(order);
        log.info("{}: orderId={}, orderNo={}, convertQty={}, convertAmt={}",
                actionLabel, order.getId(), order.getOrderNo(), orderConvertQty, orderConvertAmt);
        return order;
    }

    // ==================== 私有方法 ====================

    /** 构建单据级查询 wrapper（按单据/按明细共用） */
    private LambdaQueryWrapper<WmsBorrowOrder> buildDocWrapper(BorrowOrderQuery q) {
        LambdaQueryWrapper<WmsBorrowOrder> wrapper = new LambdaQueryWrapper<WmsBorrowOrder>()
                .eq(q.getDirection() != null, WmsBorrowOrder::getDirection, q.getDirection())
                .like(StringUtils.hasText(q.getOrderNo()), WmsBorrowOrder::getOrderNo, q.getOrderNo())
                .like(StringUtils.hasText(q.getPartnerName()), WmsBorrowOrder::getPartnerName, q.getPartnerName())
                .like(StringUtils.hasText(q.getPartnerCode()), WmsBorrowOrder::getPartnerCode, q.getPartnerCode())
                .like(StringUtils.hasText(q.getHandlerName()), WmsBorrowOrder::getHandlerName, q.getHandlerName())
                .like(StringUtils.hasText(q.getDeptName()), WmsBorrowOrder::getDeptName, q.getDeptName())
                .like(StringUtils.hasText(q.getCreatorName()), WmsBorrowOrder::getCreatorName, q.getCreatorName())
                .like(StringUtils.hasText(q.getBookkeeperName()), WmsBorrowOrder::getBookkeeperName, q.getBookkeeperName())
                .like(StringUtils.hasText(q.getRemark()), WmsBorrowOrder::getRemark, q.getRemark())
                .like(StringUtils.hasText(q.getWarehouseName()), WmsBorrowOrder::getWarehouseName, q.getWarehouseName())
                .eq(q.getWarehouseId() != null, WmsBorrowOrder::getWarehouseId, q.getWarehouseId())
                .eq(q.getStatus() != null, WmsBorrowOrder::getStatus, q.getStatus());
        if (StringUtils.hasText(q.getDateStart())) {
            wrapper.ge(WmsBorrowOrder::getBorrowDate, LocalDate.parse(q.getDateStart()));
        }
        if (StringUtils.hasText(q.getDateEnd())) {
            wrapper.le(WmsBorrowOrder::getBorrowDate, LocalDate.parse(q.getDateEnd()));
        }
        if (StringUtils.hasText(q.getReturnDateStart())) {
            wrapper.ge(WmsBorrowOrder::getExpectedReturnDate, LocalDate.parse(q.getReturnDateStart()));
        }
        if (StringUtils.hasText(q.getReturnDateEnd())) {
            wrapper.le(WmsBorrowOrder::getExpectedReturnDate, LocalDate.parse(q.getReturnDateEnd()));
        }
        return wrapper;
    }

    private WmsBorrowOrder getOrThrow(Long id) {
        WmsBorrowOrder order = orderMapper.selectById(id);
        if (order == null) throw new WmsBusinessException("借进借出单不存在: " + id);
        return order;
    }

    private List<WmsBorrowOrderItem> listItems(Long orderId) {
        LambdaQueryWrapper<WmsBorrowOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsBorrowOrderItem::getOrderId, orderId);
        wrapper.orderByAsc(WmsBorrowOrderItem::getLineNo);
        return itemMapper.selectList(wrapper);
    }

    private void saveItems(Long orderId, List<WmsBorrowOrderItem> items) {
        if (items == null) return;
        int lineNo = 1;
        for (WmsBorrowOrderItem item : items) {
            item.setId(null);
            item.setOrderId(orderId);
            item.setLineNo(lineNo++);
            if (item.getReturnedQuantity() == null) item.setReturnedQuantity(BigDecimal.ZERO);
            if (item.getProcessedReturnQuantity() == null) item.setProcessedReturnQuantity(BigDecimal.ZERO);
            if (item.getProcessedPurchaseQuantity() == null) item.setProcessedPurchaseQuantity(BigDecimal.ZERO);
            if (item.getAmount() == null && item.getQuantity() != null && item.getPrice() != null) {
                item.setAmount(item.getQuantity().multiply(item.getPrice()));
            }
            BigDecimal qty = nvl(item.getQuantity());
            BigDecimal retd = nvl(item.getReturnedQuantity());
            BigDecimal conv = nvl(item.getProcessedPurchaseQuantity());
            if (item.getNonProcessedQuantity() == null) {
                item.setNonProcessedQuantity(qty.subtract(retd).subtract(conv).max(BigDecimal.ZERO));
            }
            if (item.getNonProcessedAmount() == null) {
                item.setNonProcessedAmount(nvl(item.getNonProcessedQuantity()).multiply(nvl(item.getPrice())));
            }
            itemMapper.insert(item);
        }
    }

    /**
     * 库存增加前确保存在仓库级库存行（无库位/无批次），补齐商品信息.
     */
    private void ensureInventoryRow(WmsBorrowOrderItem item, WmsBorrowOrder order) {
        WmsInventory existing = inventoryService.getByUniqueKey(item.getProductId(), order.getWarehouseId(), null, null);
        if (existing != null) return;
        WmsInventory inventory = new WmsInventory();
        inventory.setProductId(item.getProductId());
        inventory.setProductCode(item.getProductCode());
        inventory.setProductName(item.getProductName());
        inventory.setProductSpec(item.getProductSpec());
        inventory.setProductUnit(item.getUnit());
        inventory.setWarehouseId(order.getWarehouseId());
        inventory.setWarehouseName(order.getWarehouseName());
        inventory.setQuantity(BigDecimal.ZERO);
        inventory.setAvailableQuantity(BigDecimal.ZERO);
        inventory.setFrozenQuantity(BigDecimal.ZERO);
        inventoryMapper.insert(inventory);
    }

    private BigDecimal sumQuantity(List<WmsBorrowOrderItem> items) {
        if (items == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (WmsBorrowOrderItem item : items) {
            if (item.getQuantity() != null) total = total.add(item.getQuantity());
        }
        return total;
    }

    /** 汇总借进借出单数量/金额/重量/体积/未处理 */
    private void fillTotals(WmsBorrowOrder order, List<WmsBorrowOrderItem> items) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        if (items != null) {
            for (WmsBorrowOrderItem item : items) {
                totalQty = totalQty.add(nvl(item.getQuantity()));
                totalAmt = totalAmt.add(nvl(item.getAmount()));
                totalWeight = totalWeight.add(nvl(item.getWeight()));
                totalVolume = totalVolume.add(nvl(item.getVolume()));
            }
        }
        order.setTotalQuantity(totalQty);
        order.setBorrowQuantity(totalQty);
        order.setBorrowAmount(totalAmt);
        // 初始未处理=借进总量（还出/转采购为后续动作，新建时均为0）
        order.setNonProcessedQuantity(totalQty);
        order.setNonProcessedAmount(totalAmt);
        order.setTotalWeight(totalWeight);
        order.setTotalVolume(totalVolume);
    }

    private void validateDirection(Integer direction) {
        if (direction == null || (direction != DIRECTION_IN && direction != DIRECTION_OUT)) {
            throw new WmsBusinessException("方向不合法: 1-借进 2-借出");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
