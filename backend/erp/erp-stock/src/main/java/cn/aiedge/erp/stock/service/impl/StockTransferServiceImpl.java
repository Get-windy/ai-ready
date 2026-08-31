package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.dto.StockTransferCreateDTO;
import cn.aiedge.erp.stock.dto.StockTransferItemDTO;
import cn.aiedge.erp.stock.dto.StockTransferItemVO;
import cn.aiedge.erp.stock.dto.StockTransferQuery;
import cn.aiedge.erp.stock.entity.Stock;
import cn.aiedge.erp.stock.entity.StockTransfer;
import cn.aiedge.erp.stock.entity.StockTransferItem;
import cn.aiedge.erp.stock.enums.StockTransferStatus;
import cn.aiedge.erp.stock.mapper.StockMapper;
import cn.aiedge.erp.stock.mapper.StockTransferItemMapper;
import cn.aiedge.erp.stock.mapper.StockTransferMapper;
import cn.aiedge.erp.stock.service.StockTransferService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockTransferServiceImpl extends ServiceImpl<StockTransferMapper, StockTransfer> implements StockTransferService {

    @Autowired
    private StockTransferItemMapper stockTransferItemMapper;

    @Autowired
    private StockMapper stockMapper;

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @Override
    public StockTransfer getByTransferNo(String transferNo) {
        return this.lambdaQuery()
                .eq(StockTransfer::getTransferNo, transferNo)
                .one();
    }

    @Override
    public Page<StockTransfer> pageList(StockTransferQuery q) {
        LambdaQueryWrapper<StockTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockTransfer::getDeleted, 0);
        if (hasText(q.getKeyword())) {
            wrapper.and(w -> w.like(StockTransfer::getTransferNo, q.getKeyword())
                    .or().like(StockTransfer::getSourceBillNo, q.getKeyword())
                    .or().like(StockTransfer::getApplicantName, q.getKeyword()));
        }
        wrapper.like(hasText(q.getTransferNo()), StockTransfer::getTransferNo, q.getTransferNo());
        wrapper.like(hasText(q.getSourceBillNo()), StockTransfer::getSourceBillNo, q.getSourceBillNo());
        wrapper.like(hasText(q.getApplicantName()), StockTransfer::getApplicantName, q.getApplicantName());
        wrapper.like(hasText(q.getDepartmentName()), StockTransfer::getDepartmentName, q.getDepartmentName());
        wrapper.like(hasText(q.getCreateByName()), StockTransfer::getCreateByName, q.getCreateByName());
        wrapper.like(hasText(q.getPosterName()), StockTransfer::getPosterName, q.getPosterName());
        wrapper.like(hasText(q.getFromWarehouseName()), StockTransfer::getFromWarehouseName, q.getFromWarehouseName());
        wrapper.like(hasText(q.getToWarehouseName()), StockTransfer::getToWarehouseName, q.getToWarehouseName());
        wrapper.like(hasText(q.getRemark()), StockTransfer::getRemark, q.getRemark());
        wrapper.eq(q.getStatus() != null, StockTransfer::getStatus, q.getStatus());
        wrapper.eq(q.getTransferType() != null, StockTransfer::getTransferType, q.getTransferType());
        wrapper.eq(q.getFromWarehouseId() != null, StockTransfer::getFromWarehouseId, q.getFromWarehouseId());
        wrapper.eq(q.getToWarehouseId() != null, StockTransfer::getToWarehouseId, q.getToWarehouseId());
        wrapper.ge(q.getStartDate() != null, StockTransfer::getBillDate, q.getStartDate());
        wrapper.le(q.getEndDate() != null, StockTransfer::getBillDate, q.getEndDate());
        wrapper.orderByDesc(StockTransfer::getCreateTime);
        return this.page(new Page<>(q.getPageNum(), q.getPageSize()), wrapper);
    }

    @Override
    public Page<StockTransferItemVO> pageDetail(StockTransferQuery q) {
        // 1. 先取满足单据级过滤的单据ID集合
        LambdaQueryWrapper<StockTransfer> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(StockTransfer::getDeleted, 0);
        if (hasText(q.getKeyword())) {
            docWrapper.and(w -> w.like(StockTransfer::getTransferNo, q.getKeyword())
                    .or().like(StockTransfer::getSourceBillNo, q.getKeyword())
                    .or().like(StockTransfer::getApplicantName, q.getKeyword()));
        }
        docWrapper.like(hasText(q.getTransferNo()), StockTransfer::getTransferNo, q.getTransferNo());
        docWrapper.like(hasText(q.getSourceBillNo()), StockTransfer::getSourceBillNo, q.getSourceBillNo());
        docWrapper.like(hasText(q.getApplicantName()), StockTransfer::getApplicantName, q.getApplicantName());
        docWrapper.like(hasText(q.getDepartmentName()), StockTransfer::getDepartmentName, q.getDepartmentName());
        docWrapper.like(hasText(q.getCreateByName()), StockTransfer::getCreateByName, q.getCreateByName());
        docWrapper.like(hasText(q.getPosterName()), StockTransfer::getPosterName, q.getPosterName());
        docWrapper.like(hasText(q.getFromWarehouseName()), StockTransfer::getFromWarehouseName, q.getFromWarehouseName());
        docWrapper.like(hasText(q.getToWarehouseName()), StockTransfer::getToWarehouseName, q.getToWarehouseName());
        docWrapper.like(hasText(q.getRemark()), StockTransfer::getRemark, q.getRemark());
        docWrapper.eq(q.getStatus() != null, StockTransfer::getStatus, q.getStatus());
        docWrapper.eq(q.getTransferType() != null, StockTransfer::getTransferType, q.getTransferType());
        docWrapper.eq(q.getFromWarehouseId() != null, StockTransfer::getFromWarehouseId, q.getFromWarehouseId());
        docWrapper.eq(q.getToWarehouseId() != null, StockTransfer::getToWarehouseId, q.getToWarehouseId());
        docWrapper.ge(q.getStartDate() != null, StockTransfer::getBillDate, q.getStartDate());
        docWrapper.le(q.getEndDate() != null, StockTransfer::getBillDate, q.getEndDate());
        docWrapper.select(StockTransfer::getId);
        List<StockTransfer> docs = this.list(docWrapper);
        List<Long> docIds = docs.stream().map(StockTransfer::getId).collect(Collectors.toList());
        if (docIds.isEmpty()) {
            return new Page<>(q.getPageNum(), q.getPageSize(), 0);
        }

        // 2. 明细过滤
        LambdaQueryWrapper<StockTransferItem> itemWrapper = new LambdaQueryWrapper<StockTransferItem>()
                .in(StockTransferItem::getTransferId, docIds)
                .like(hasText(q.getProductName()), StockTransferItem::getProductName, q.getProductName())
                .like(hasText(q.getItemRemark()), StockTransferItem::getRemark, q.getItemRemark())
                .orderByDesc(StockTransferItem::getCreateTime);
        Page<StockTransferItem> itemPage = stockTransferItemMapper.selectPage(
                new Page<>(q.getPageNum(), q.getPageSize()), itemWrapper);

        // 3. 批量补齐单据级字段
        List<Long> pageDocIds = itemPage.getRecords().stream()
                .map(StockTransferItem::getTransferId).distinct().collect(Collectors.toList());
        Map<Long, StockTransfer> docMap = pageDocIds.isEmpty() ? Map.of() :
                this.listByIds(pageDocIds).stream().collect(Collectors.toMap(StockTransfer::getId, Function.identity()));

        List<StockTransferItemVO> voList = new ArrayList<>();
        for (StockTransferItem item : itemPage.getRecords()) {
            StockTransferItemVO vo = new StockTransferItemVO();
            BeanUtils.copyProperties(item, vo);
            StockTransfer doc = docMap.get(item.getTransferId());
            if (doc != null) {
                vo.setBillDate(doc.getBillDate());
                vo.setTransferNo(doc.getTransferNo());
                vo.setStatus(doc.getStatus());
                vo.setTransferType(doc.getTransferType());
                vo.setFromWarehouseId(doc.getFromWarehouseId());
                vo.setFromWarehouseName(doc.getFromWarehouseName());
                vo.setToWarehouseId(doc.getToWarehouseId());
                vo.setToWarehouseName(doc.getToWarehouseName());
                vo.setHandlerName(doc.getHandlerName() != null ? doc.getHandlerName() : doc.getApplicantName());
                vo.setApplicationName(doc.getApplicantName());
                vo.setDepartmentName(doc.getDepartmentName());
                vo.setSourceBillNo(doc.getSourceBillNo());
                vo.setDocRemark(doc.getRemark());
                vo.setSummary(doc.getSummary());
                vo.setAttachment(doc.getAttachment());
                vo.setPosterName(doc.getPosterName());
                vo.setCreateByName(doc.getCreateByName());
                vo.setPosterTime(doc.getPosterTime());
                vo.setCreateTime(doc.getCreateTime());
                vo.setPrintCount(doc.getPrintCount());
            }
            voList.add(vo);
        }
        Page<StockTransferItemVO> voPage = new Page<>(q.getPageNum(), q.getPageSize(), itemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public StockTransfer getDetail(Long id) {
        StockTransfer transfer = this.getById(id);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        transfer.setExtInfo(null);
        transfer.setItems(getItems(id));
        return transfer;
    }

    @Override
    public Page<StockTransfer> pageList(String keyword, Long fromWarehouseId, Long toWarehouseId, Integer status, int pageNum, int pageSize) {
        StockTransferQuery q = new StockTransferQuery();
        q.setKeyword(keyword);
        q.setFromWarehouseId(fromWarehouseId);
        q.setToWarehouseId(toWarehouseId);
        q.setStatus(status);
        q.setPageNum(pageNum);
        q.setPageSize(pageSize);
        return pageList(q);
    }

    @Override
    public List<StockTransfer> exportList(StockTransferQuery q) {
        LambdaQueryWrapper<StockTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockTransfer::getDeleted, 0);
        wrapper.like(hasText(q.getKeyword()), StockTransfer::getTransferNo, q.getKeyword());
        wrapper.like(hasText(q.getTransferNo()), StockTransfer::getTransferNo, q.getTransferNo());
        wrapper.like(hasText(q.getSourceBillNo()), StockTransfer::getSourceBillNo, q.getSourceBillNo());
        wrapper.like(hasText(q.getApplicantName()), StockTransfer::getApplicantName, q.getApplicantName());
        wrapper.like(hasText(q.getDepartmentName()), StockTransfer::getDepartmentName, q.getDepartmentName());
        wrapper.like(hasText(q.getCreateByName()), StockTransfer::getCreateByName, q.getCreateByName());
        wrapper.like(hasText(q.getPosterName()), StockTransfer::getPosterName, q.getPosterName());
        wrapper.like(hasText(q.getFromWarehouseName()), StockTransfer::getFromWarehouseName, q.getFromWarehouseName());
        wrapper.like(hasText(q.getToWarehouseName()), StockTransfer::getToWarehouseName, q.getToWarehouseName());
        wrapper.like(hasText(q.getRemark()), StockTransfer::getRemark, q.getRemark());
        wrapper.eq(q.getStatus() != null, StockTransfer::getStatus, q.getStatus());
        wrapper.eq(q.getTransferType() != null, StockTransfer::getTransferType, q.getTransferType());
        wrapper.eq(q.getFromWarehouseId() != null, StockTransfer::getFromWarehouseId, q.getFromWarehouseId());
        wrapper.eq(q.getToWarehouseId() != null, StockTransfer::getToWarehouseId, q.getToWarehouseId());
        wrapper.ge(q.getStartDate() != null, StockTransfer::getBillDate, q.getStartDate());
        wrapper.le(q.getEndDate() != null, StockTransfer::getBillDate, q.getEndDate());
        wrapper.orderByDesc(StockTransfer::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<StockTransfer> listByFromWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(StockTransfer::getFromWarehouseId, warehouseId)
                .orderByDesc(StockTransfer::getCreateTime)
                .list();
    }

    @Override
    public List<StockTransfer> listByToWarehouseId(Long warehouseId) {
        return this.lambdaQuery()
                .eq(StockTransfer::getToWarehouseId, warehouseId)
                .orderByDesc(StockTransfer::getCreateTime)
                .list();
    }

    @Override
    public String generateTransferNo() {
        String prefix = "DB";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<StockTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(StockTransfer::getTransferNo, prefix + dateStr)
                .eq(StockTransfer::getDeleted, 0)
                .orderByDesc(StockTransfer::getTransferNo)
                .last("LIMIT 1");
        StockTransfer last = this.getOne(wrapper);
        int seq = 1;
        if (last != null && last.getTransferNo() != null && last.getTransferNo().length() >= prefix.length() + dateStr.length() + 1) {
            String lastNo = last.getTransferNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    private StockTransferItem toItem(StockTransferItemDTO dto, Long tenantId, Long transferId, int lineNo) {
        StockTransferItem item = new StockTransferItem();
        BeanUtils.copyProperties(dto, item);
        item.setTenantId(tenantId);
        item.setTransferId(transferId);
        item.setLineNo(lineNo);
        item.setStatus(0);
        item.setCreateTime(LocalDateTime.now());
        calculateItemAmounts(item);
        return item;
    }

    private void calculateItemAmounts(StockTransferItem item) {
        BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
        BigDecimal transferPrice = item.getTransferPrice() != null ? item.getTransferPrice() : BigDecimal.ZERO;
        if (item.getCostAmount() == null) {
            item.setCostAmount(qty.multiply(unitCost));
        }
        if (item.getTransferAmount() == null) {
            item.setTransferAmount(qty.multiply(transferPrice));
        }
        BigDecimal costAmount = item.getCostAmount() != null ? item.getCostAmount() : BigDecimal.ZERO;
        BigDecimal transferAmount = item.getTransferAmount() != null ? item.getTransferAmount() : BigDecimal.ZERO;
        if (item.getTransferDiff() == null) {
            item.setTransferDiff(transferAmount.subtract(costAmount));
        }
        if (item.getLineAmount() == null) {
            item.setLineAmount(transferAmount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer createTransfer(StockTransferCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        StockTransfer transfer = new StockTransfer();
        BeanUtils.copyProperties(dto, transfer);
        transfer.setTenantId(tenantId);
        transfer.setTransferNo(generateTransferNo());
        transfer.setStatus(StockTransferStatus.DRAFT.getCode());
        transfer.setCreateTime(LocalDateTime.now());
        transfer.setCreateBy(tenantId);
        transfer.setApplicantId(dto.getApplicantId() != null ? dto.getApplicantId() : tenantId);
        if (dto.getApplicantName() == null) transfer.setApplicantName(transfer.getCreateByName());
        this.save(transfer);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (int i = 0; i < dto.getItems().size(); i++) {
                StockTransferItem item = toItem(dto.getItems().get(i), tenantId, transfer.getId(), i + 1);
                stockTransferItemMapper.insert(item);
            }
        }

        calculateTotals(transfer.getId());
        return this.getById(transfer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer updateTransfer(Long transferId, StockTransferCreateDTO dto) {
        StockTransfer existing = this.getById(transferId);
        if (existing == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (existing.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的调拨单可以修改");
        }
        Long tenantId = existing.getTenantId() != null ? existing.getTenantId() : StpUtil.getLoginIdAsLong();
        StockTransfer update = new StockTransfer();
        BeanUtils.copyProperties(dto, update);
        update.setId(transferId);
        update.setTenantId(tenantId);
        update.setTransferNo(existing.getTransferNo());
        update.setStatus(existing.getStatus());
        update.setCreateTime(existing.getCreateTime());
        update.setCreateBy(existing.getCreateBy());
        this.updateById(update);

        // 全量替换明细
        stockTransferItemMapper.delete(new LambdaQueryWrapper<StockTransferItem>()
                .eq(StockTransferItem::getTransferId, transferId));
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (int i = 0; i < dto.getItems().size(); i++) {
                StockTransferItem item = toItem(dto.getItems().get(i), tenantId, transferId, i + 1);
                stockTransferItemMapper.insert(item);
            }
        }

        calculateTotals(transferId);
        return this.getById(transferId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer submitForApproval(Long transferId) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的调拨单可以提交审批");
        }

        transfer.setStatus(StockTransferStatus.PENDING_APPROVAL.getCode());
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer approve(Long transferId, Long approverId, String note) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的调拨单可以审批");
        }

        transfer.setStatus(StockTransferStatus.APPROVED.getCode());
        transfer.setApprovedBy(approverId);
        transfer.setApprovedTime(LocalDateTime.now());
        transfer.setApprovedNote(note);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer reject(Long transferId, String reason) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的调拨单可以拒绝");
        }

        transfer.setStatus(StockTransferStatus.REJECTED.getCode());
        transfer.setApprovedNote(reason);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer execute(Long transferId) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.APPROVED.getCode()) {
            throw BusinessException.badRequest("只有已审批状态的调拨单可以执行");
        }

        List<StockTransferItem> items = getItems(transferId);
        Long userId = StpUtil.getLoginIdAsLong();

        for (StockTransferItem item : items) {
            Stock fromStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, transfer.getFromWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );

            if (fromStock == null || fromStock.getQuantity().compareTo(item.getQuantity()) < 0) {
                throw BusinessException.badRequest("源仓库库存不足: " + item.getProductName());
            }

            fromStock.setQuantity(fromStock.getQuantity().subtract(item.getQuantity()));
            fromStock.setUpdateTime(LocalDateTime.now());
            fromStock.setUpdateBy(userId);
            stockMapper.updateById(fromStock);

            Stock toStock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>()
                            .eq(Stock::getWarehouseId, transfer.getToWarehouseId())
                            .eq(Stock::getProductId, item.getProductId())
            );

            if (toStock == null) {
                toStock = new Stock();
                toStock.setTenantId(transfer.getTenantId());
                toStock.setWarehouseId(transfer.getToWarehouseId());
                toStock.setProductId(item.getProductId());
                toStock.setProductName(item.getProductName());
                toStock.setProductCode(item.getProductCode());
                toStock.setQuantity(item.getQuantity());
                toStock.setCreateTime(LocalDateTime.now());
                toStock.setCreateBy(userId);
                stockMapper.insert(toStock);
            } else {
                toStock.setQuantity(toStock.getQuantity().add(item.getQuantity()));
                toStock.setUpdateTime(LocalDateTime.now());
                toStock.setUpdateBy(userId);
                stockMapper.updateById(toStock);
            }

            item.setStatus(1);
            item.setUpdateTime(LocalDateTime.now());
            stockTransferItemMapper.updateById(item);
        }

        transfer.setStatus(StockTransferStatus.COMPLETED.getCode());
        transfer.setExecuteBy(userId);
        transfer.setExecuteTime(LocalDateTime.now());
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);

        return transfer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransfer cancel(Long transferId, String reason) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() == StockTransferStatus.COMPLETED.getCode()) {
            throw BusinessException.badRequest("已完成的调拨单不能取消");
        }

        transfer.setStatus(StockTransferStatus.CANCELLED.getCode());
        transfer.setRemark(reason);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
        return transfer;
    }

    @Override
    public List<StockTransferItem> getItems(Long transferId) {
        return stockTransferItemMapper.selectList(
                new LambdaQueryWrapper<StockTransferItem>()
                        .eq(StockTransferItem::getTransferId, transferId)
                        .orderByAsc(StockTransferItem::getLineNo)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransferItem addItem(Long transferId, StockTransferItem item) {
        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) {
            throw BusinessException.notFound("调拨单不存在");
        }
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的调拨单可以添加明细");
        }

        Long tenantId = StpUtil.getLoginIdAsLong();
        item.setTenantId(tenantId);
        item.setTransferId(transferId);
        item.setStatus(0);
        item.setCreateTime(LocalDateTime.now());
        stockTransferItemMapper.insert(item);

        calculateTotals(transferId);

        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockTransferItem updateItem(Long itemId, StockTransferItem item) {
        StockTransferItem existing = stockTransferItemMapper.selectById(itemId);
        if (existing == null) {
            throw BusinessException.notFound("调拨明细不存在");
        }

        StockTransfer transfer = this.getById(existing.getTransferId());
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的调拨单可以修改明细");
        }

        existing.setProductId(item.getProductId());
        existing.setProductName(item.getProductName());
        existing.setProductCode(item.getProductCode());
        existing.setQuantity(item.getQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        existing.setRemark(item.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        stockTransferItemMapper.updateById(existing);

        calculateTotals(existing.getTransferId());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        StockTransferItem item = stockTransferItemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("调拨明细不存在");
        }

        StockTransfer transfer = this.getById(item.getTransferId());
        if (transfer.getStatus() != StockTransferStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的调拨单可以删除明细");
        }

        stockTransferItemMapper.deleteById(itemId);

        calculateTotals(item.getTransferId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long transferId) {
        List<StockTransferItem> items = getItems(transferId);

        int totalItems = items.size();
        BigDecimal totalQuantity = items.stream()
                .map(i -> nvl(i.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream()
                .map(i -> nvl(i.getTransferAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCostAmount = items.stream()
                .map(i -> nvl(i.getCostAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransferDiff = items.stream()
                .map(i -> nvl(i.getTransferDiff())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = items.stream()
                .map(i -> nvl(i.getWeight())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVolume = items.stream()
                .map(i -> nvl(i.getVolume())).reduce(BigDecimal.ZERO, BigDecimal::add);

        StockTransfer transfer = this.getById(transferId);
        if (transfer == null) return;
        transfer.setTotalItems(totalItems);
        transfer.setTotalQuantity(totalQuantity);
        transfer.setTotalAmount(totalAmount);
        transfer.setTotalCostAmount(totalCostAmount);
        transfer.setTotalTransferDiff(totalTransferDiff);
        transfer.setTotalWeight(totalWeight);
        transfer.setTotalVolume(totalVolume);
        transfer.setUpdateTime(LocalDateTime.now());
        this.updateById(transfer);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
