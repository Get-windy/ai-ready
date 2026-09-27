package cn.aiedge.crm.quotation.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.service.CustomerService;
import cn.aiedge.crm.quotation.entity.Quotation;
import cn.aiedge.crm.quotation.entity.QuotationItem;
import cn.aiedge.crm.quotation.enums.QuotationStatus;
import cn.aiedge.crm.quotation.mapper.QuotationItemMapper;
import cn.aiedge.crm.quotation.mapper.QuotationMapper;
import cn.aiedge.crm.quotation.service.QuotationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationServiceImpl extends ServiceImpl<QuotationMapper, Quotation> implements QuotationService {

    private final QuotationItemMapper quotationItemMapper;
    /** 系统统一号段服务（biz_number_sequence，行锁 + 按日重置） */
    private final BizNumberGeneratorService bizNumberGeneratorService;

    /** 转订单时需把 CRM 客户解析为 ERP 往来单位（红线：ERP 单据的 customer_id 必须是 biz_party.id） */
    private final CustomerService customerService;

    @Override
    public Quotation getByQuotationNo(String quotationNo) {
        return lambdaQuery()
                .eq(Quotation::getQuotationNo, quotationNo)
                .eq(Quotation::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Quotation> pageList(String keyword, Long customerId, Long opportunityId, Integer status, Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Quotation> wrapper = buildQueryWrapper(keyword, customerId, opportunityId, status, salesPersonId);
        wrapper.orderByDesc(Quotation::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Quotation> exportList(String keyword, Long customerId, Long opportunityId, Integer status, Long salesPersonId) {
        LambdaQueryWrapper<Quotation> wrapper = buildQueryWrapper(keyword, customerId, opportunityId, status, salesPersonId);
        wrapper.orderByDesc(Quotation::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 构建公共查询条件
     */
    private LambdaQueryWrapper<Quotation> buildQueryWrapper(String keyword, Long customerId, Long opportunityId,
                                                             Integer status, Long salesPersonId) {
        LambdaQueryWrapper<Quotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Quotation::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Quotation::getQuotationNo, keyword)
                    .or().like(Quotation::getTitle, keyword)
                    .or().like(Quotation::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(Quotation::getCustomerId, customerId);
        }
        if (opportunityId != null) {
            wrapper.eq(Quotation::getOpportunityId, opportunityId);
        }
        if (status != null) {
            wrapper.eq(Quotation::getStatus, status);
        }
        if (salesPersonId != null) {
            wrapper.eq(Quotation::getSalesPersonId, salesPersonId);
        }
        return wrapper;
    }

    @Override
    public List<Quotation> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<Quotation> listByOpportunityId(Long opportunityId) {
        return baseMapper.selectByOpportunityId(opportunityId);
    }

    @Override
    public List<Quotation> listVersions(Long parentId) {
        return baseMapper.selectVersionsByParentId(parentId);
    }

    @Override
    public String generateQuotationNo() {
        // 走系统统一号段（biz_number_sequence + SELECT FOR UPDATE）：原实现「查当日最大号 +1」既有并发竞态，
        // 又带 deleted=0 条件 —— 逻辑删除的行不计数但占号，会撞唯一索引。
        return bizNumberGeneratorService.nextNumber("CRM_QUOTATION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation createQuotation(Quotation quotation, List<QuotationItem> items) {
        quotation.setQuotationNo(generateQuotationNo());
        quotation.setStatus(QuotationStatus.DRAFT.getCode());
        quotation.setVersion(1);
        if (quotation.getParentId() == null) {
            quotation.setParentId(quotation.getId());
        }
        save(quotation);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                QuotationItem item = items.get(i);
                item.setQuotationId(quotation.getId());
                item.setLineNo(i + 1);
                item.setTenantId(quotation.getTenantId());
                calculateLineAmount(item);
                quotationItemMapper.insert(item);
            }
        }
        calculateAmounts(quotation.getId());
        return getById(quotation.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation createFromOpportunity(Long opportunityId) {
        Quotation quotation = new Quotation();
        quotation.setOpportunityId(opportunityId);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setValidFrom(LocalDate.now());
        quotation.setValidTo(LocalDate.now().plusDays(30));
        quotation.setQuotationType(1);
        quotation.setTitle("商机报价单");
        return createQuotation(quotation, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation createFromTemplate(Long templateId, Long customerId) {
        return createQuotation(new Quotation(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation copyQuotation(Long quotationId) {
        Quotation source = getById(quotationId);
        if (source == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        Quotation copy = new Quotation();
        copy.setCustomerId(source.getCustomerId());
        copy.setCustomerName(source.getCustomerName());
        copy.setOpportunityId(source.getOpportunityId());
        copy.setContactId(source.getContactId());
        copy.setQuotationDate(LocalDate.now());
        copy.setValidFrom(LocalDate.now());
        copy.setValidTo(LocalDate.now().plusDays(30));
        copy.setQuotationType(source.getQuotationType());
        copy.setTitle(source.getTitle() + "(副本)");
        copy.setDescription(source.getDescription());
        copy.setPaymentTerms(source.getPaymentTerms());
        copy.setPaymentDays(source.getPaymentDays());
        copy.setDeliveryTerms(source.getDeliveryTerms());
        copy.setDeliveryDays(source.getDeliveryDays());
        copy.setDeliveryAddress(source.getDeliveryAddress());
        copy.setReceiverName(source.getReceiverName());
        copy.setReceiverPhone(source.getReceiverPhone());
        copy.setSalesPersonId(source.getSalesPersonId());
        copy.setSalesPersonName(source.getSalesPersonName());
        copy.setDepartmentId(source.getDepartmentId());
        copy.setDepartmentName(source.getDepartmentName());
        List<QuotationItem> sourceItems = getItems(quotationId);
        return createQuotation(copy, sourceItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation createNewVersion(Long quotationId) {
        Quotation source = getById(quotationId);
        if (source == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        Long parentId = source.getParentId() != null ? source.getParentId() : source.getId();
        Integer maxVersion = baseMapper.selectMaxVersionByParentId(parentId);
        int newVersion = (maxVersion != null ? maxVersion : 1) + 1;
        Quotation newQuotation = new Quotation();
        newQuotation.setParentId(parentId);
        newQuotation.setVersion(newVersion);
        newQuotation.setCustomerId(source.getCustomerId());
        newQuotation.setCustomerName(source.getCustomerName());
        newQuotation.setOpportunityId(source.getOpportunityId());
        newQuotation.setContactId(source.getContactId());
        newQuotation.setQuotationDate(LocalDate.now());
        newQuotation.setValidFrom(LocalDate.now());
        newQuotation.setValidTo(LocalDate.now().plusDays(30));
        newQuotation.setQuotationType(source.getQuotationType());
        newQuotation.setTitle(source.getTitle() + " V" + newVersion);
        newQuotation.setDescription(source.getDescription());
        newQuotation.setPaymentTerms(source.getPaymentTerms());
        newQuotation.setPaymentDays(source.getPaymentDays());
        newQuotation.setDeliveryTerms(source.getDeliveryTerms());
        newQuotation.setDeliveryDays(source.getDeliveryDays());
        newQuotation.setDeliveryAddress(source.getDeliveryAddress());
        newQuotation.setReceiverName(source.getReceiverName());
        newQuotation.setReceiverPhone(source.getReceiverPhone());
        newQuotation.setSalesPersonId(source.getSalesPersonId());
        newQuotation.setSalesPersonName(source.getSalesPersonName());
        newQuotation.setDepartmentId(source.getDepartmentId());
        newQuotation.setDepartmentName(source.getDepartmentName());
        List<QuotationItem> sourceItems = getItems(quotationId);
        return createQuotation(newQuotation, sourceItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation updateQuotation(Long quotationId, Quotation quotation, List<QuotationItem> items) {
        Quotation existing = getById(quotationId);
        if (existing == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (existing.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以修改");
        }
        quotation.setId(quotationId);
        updateById(quotation);
        if (items != null) {
            List<QuotationItem> existingItems = getItems(quotationId);
            for (QuotationItem oldItem : existingItems) {
                quotationItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                QuotationItem item = items.get(i);
                item.setQuotationId(quotationId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                calculateLineAmount(item);
                quotationItemMapper.insert(item);
            }
        }
        calculateAmounts(quotationId);
        return getById(quotationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation submitForApproval(Long quotationId) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以提交审批");
        }
        quotation.setStatus(QuotationStatus.PENDING_APPROVAL.getCode());
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation approve(Long quotationId, Long approverId, String note) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的报价单可以审批");
        }
        quotation.setStatus(QuotationStatus.APPROVED.getCode());
        quotation.setApprovedBy(approverId);
        quotation.setApprovedTime(LocalDateTime.now());
        quotation.setApprovedNote(note);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation reject(Long quotationId, Long rejecterId, String reason) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的报价单可以拒绝");
        }
        quotation.setStatus(QuotationStatus.DRAFT.getCode());
        quotation.setRejectedBy(rejecterId);
        quotation.setRejectedTime(LocalDateTime.now());
        quotation.setRejectedReason(reason);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation sendToCustomer(Long quotationId, Long senderId, String method) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.APPROVED.getCode()) {
            throw BusinessException.badRequest("只有已审批状态的报价单可以发送");
        }
        quotation.setStatus(QuotationStatus.SENT.getCode());
        quotation.setSentBy(senderId);
        quotation.setSentTime(LocalDateTime.now());
        quotation.setSentMethod(method);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation markAccepted(Long quotationId, Long accepterId, String note) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.SENT.getCode()) {
            throw BusinessException.badRequest("只有已发送状态的报价单可以标记为已接受");
        }
        quotation.setStatus(QuotationStatus.ACCEPTED.getCode());
        quotation.setAcceptedBy(accepterId);
        quotation.setAcceptedTime(LocalDateTime.now());
        quotation.setAcceptedNote(note);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation markRejected(Long quotationId, Long rejecterId, String reason) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.SENT.getCode()) {
            throw BusinessException.badRequest("只有已发送状态的报价单可以标记为已拒绝");
        }
        quotation.setStatus(QuotationStatus.REJECTED.getCode());
        quotation.setRejectedBy(rejecterId);
        quotation.setRejectedTime(LocalDateTime.now());
        quotation.setRejectedReason(reason);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation convertToOrder(Long quotationId) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.ACCEPTED.getCode()) {
            throw BusinessException.badRequest("只有已接受状态的报价单可以转订单");
        }

        // 先创建销售订单（如失败则事务整体回滚，报价状态不受影响）
        createSaleOrder(quotation);

        // 创建成功后再更新报价为已转换状态
        quotation.setStatus(QuotationStatus.CONVERTED.getCode());
        quotation.setConvertedBy(quotation.getCreateBy());
        quotation.setConvertedTime(LocalDateTime.now());
        updateById(quotation);

        log.info("报价单 {} 已转为销售订单，orderNo={}", quotation.getQuotationNo(), quotation.getOrderNo());
        return quotation;
    }

    private void createSaleOrder(Quotation quotation) {
        // ⚠️ 红线校验（CRM README §红线）：ERP 单据的 customer_id 必须是 **ERP 往来单位** ID，
        //    绝不能把 CRM 客户 ID 写进去 —— 两者 ID 域不同，混用会让订单挂到错误的交易主体上
        //    （2026-09-23 实测：CRM 客户 1 写进 erp_sale_order 后，订单显示客户为 biz_party#1「散客（零售默认）」，
        //     而 customer_name 列却是 CRM 客户名，ID 与名称自相矛盾）。
        //    因此这里要求 CRM 客户必须先通过 md_partner_id 关联到 ERP 往来单位，否则拒绝转单。
        Customer crmCustomer = quotation.getCustomerId() == null
                ? null : customerService.getById(quotation.getCustomerId());
        if (crmCustomer == null) {
            throw BusinessException.badRequest("报价单未关联有效客户，不能转订单");
        }
        if (crmCustomer.getMdPartnerId() == null) {
            throw BusinessException.badRequest(
                    "客户「" + crmCustomer.getCustomerName() + "」尚未关联 ERP 往来单位，不能转订单。"
                            + "请先在「资料 → 往来单位」建档，并回填该 CRM 客户的关联往来单位后再转单。");
        }

        // 用 Java 生成单一订单 ID，避免 NEXTVAL/CURRVAL 关联问题
        long orderId = (System.currentTimeMillis() << 10 | ThreadLocalRandom.current().nextInt(1024));
        String orderNo = generateSaleOrderNo();

        // 插入订单头（customer_id 用解析后的 ERP 往来单位 ID）
        baseMapper.insertSaleOrderRaw(orderId, orderNo, quotation, crmCustomer.getMdPartnerId());
        // 插入明细行
        List<QuotationItem> items = quotationItemMapper.selectList(
            new LambdaQueryWrapper<QuotationItem>()
                .eq(QuotationItem::getQuotationId, quotation.getId())
                .eq(QuotationItem::getDeleted, 0)
        );
        for (int i = 0; i < items.size(); i++) {
            QuotationItem item = items.get(i);
            baseMapper.insertSaleOrderItem(
                    (System.currentTimeMillis() << 10 | ThreadLocalRandom.current().nextInt(1024)),
                    orderId, i + 1, item, quotation.getTenantId(), quotation.getCreateBy());
        }
    }

    /**
     * 销售订单号：直接复用 ERP 销售模块的号段（{@code bizNumberGeneratorService.nextSaleOrderNo()}）。
     *
     * <p>原实现是 {@code "QO" + 日期 + orderId % 10000} —— 对毫秒级 ID 取模后只剩 4 位，同一自然日内
     * 很快会算出重复号；而 {@code erp_sale_order.order_no} 没有唯一索引，撞号既不报错也不告警。
     * 改为与 ERP 侧同一个号段，既消除碰撞、又让 CRM 转出的订单号与 ERP 自建的保持同一形态。</p>
     */
    private String generateSaleOrderNo() {
        return bizNumberGeneratorService.nextSaleOrderNo();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Quotation cancel(Long quotationId, String reason) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() == QuotationStatus.CONVERTED.getCode()) {
            throw BusinessException.badRequest("已转订单的报价单不能取消");
        }
        quotation.setStatus(QuotationStatus.CANCELLED.getCode());
        quotation.setRemark(reason);
        updateById(quotation);
        return quotation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateAmounts(Long quotationId) {
        List<QuotationItem> items = getItems(quotationId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (QuotationItem item : items) {
            totalAmount = totalAmount.add(item.getLineAmount());
            totalTax = totalTax.add(item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO);
            totalDiscount = totalDiscount.add(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO);
        }
        Quotation quotation = getById(quotationId);
        quotation.setTotalAmount(totalAmount);
        quotation.setDiscountAmount(totalDiscount);
        quotation.setTaxAmount(totalTax);
        BigDecimal finalAmount = totalAmount.subtract(totalDiscount).add(totalTax);
        quotation.setFinalAmount(finalAmount);
        updateById(quotation);
    }

    @Override
    public BigDecimal calculateLineAmount(QuotationItem item) {
        BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setLineAmount(lineAmount);
        BigDecimal discountRate = item.getDiscountRate() != null ? item.getDiscountRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = lineAmount.multiply(discountRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setDiscountAmount(discountAmount);
        BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;
        BigDecimal afterDiscount = lineAmount.subtract(discountAmount);
        BigDecimal taxAmount = afterDiscount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setTaxAmount(taxAmount);
        BigDecimal lineTotal = afterDiscount.add(taxAmount);
        item.setLineTotal(lineTotal);
        return lineTotal;
    }

    @Override
    public List<Quotation> getExpiredQuotations() {
        return baseMapper.selectExpiredQuotations(LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markExpiredQuotations() {
        List<Quotation> expired = getExpiredQuotations();
        for (Quotation quotation : expired) {
            quotation.setStatus(QuotationStatus.EXPIRED.getCode());
            updateById(quotation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationItem addItem(Long quotationId, QuotationItem item) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以添加明细");
        }
        Integer count = quotationItemMapper.countByQuotationId(quotationId);
        item.setQuotationId(quotationId);
        item.setLineNo(count + 1);
        item.setTenantId(quotation.getTenantId());
        calculateLineAmount(item);
        quotationItemMapper.insert(item);
        calculateAmounts(quotationId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationItem updateItem(Long itemId, QuotationItem item) {
        QuotationItem existing = quotationItemMapper.selectById(itemId);
        if (existing == null) {
            throw BusinessException.notFound("报价明细不存在");
        }
        Quotation quotation = getById(existing.getQuotationId());
        if (quotation.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以修改明细");
        }
        item.setId(itemId);
        calculateLineAmount(item);
        quotationItemMapper.updateById(item);
        calculateAmounts(existing.getQuotationId());
        return quotationItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        QuotationItem item = quotationItemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.notFound("报价明细不存在");
        }
        Quotation quotation = getById(item.getQuotationId());
        if (quotation.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以删除明细");
        }
        quotationItemMapper.deleteById(itemId);
        calculateAmounts(item.getQuotationId());
    }

    @Override
    public List<QuotationItem> getItems(Long quotationId) {
        return quotationItemMapper.selectByQuotationId(quotationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderItems(Long quotationId, List<Long> itemIds) {
        Quotation quotation = getById(quotationId);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        if (quotation.getStatus() != QuotationStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的报价单可以调整明细顺序");
        }
        for (int i = 0; i < itemIds.size(); i++) {
            QuotationItem item = quotationItemMapper.selectById(itemIds.get(i));
            if (item != null && item.getQuotationId().equals(quotationId)) {
                item.setLineNo(i + 1);
                quotationItemMapper.updateById(item);
            }
        }
    }
}