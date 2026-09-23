package cn.aiedge.crm.quotation.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.quotation.dto.QuotationCreateDTO;
import cn.aiedge.crm.quotation.dto.QuotationItemDTO;
import cn.aiedge.crm.quotation.dto.QuotationVO;
import cn.aiedge.crm.quotation.entity.Quotation;
import cn.aiedge.crm.quotation.entity.QuotationItem;
import cn.aiedge.crm.quotation.enums.QuotationStatus;
import cn.aiedge.crm.quotation.service.QuotationService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/crm/quotation")
@RequiredArgsConstructor
@Tag(name = "CRM报价管理", description = "报价单创建、审批、发送、转订单等操作")
public class QuotationController {

    private final QuotationService quotationService;

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/page")
    @Operation(summary = "分页查询报价单")
    public Page<QuotationVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机ID") @RequestParam(required = false) Long opportunityId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Quotation> page = quotationService.pageList(keyword, customerId, opportunityId, status, salesPersonId, pageNum, pageSize);
        Page<QuotationVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/{id}")
    @Operation(summary = "获取报价单详情")
    public QuotationVO getById(@PathVariable Long id) {
        Quotation quotation = quotationService.getById(id);
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        QuotationVO vo = convertToVO(quotation);
        List<QuotationItem> items = quotationService.getItems(id);
        vo.setItemCount(items.size());
        return vo;
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取报价单明细")
    public List<QuotationItem> getItems(@PathVariable Long id) {
        return quotationService.getItems(id);
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的报价单列表")
    public List<QuotationVO> listByCustomerId(@PathVariable Long customerId) {
        return quotationService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/opportunity/{opportunityId}")
    @Operation(summary = "获取商机的报价单列表")
    public List<QuotationVO> listByOpportunityId(@PathVariable Long opportunityId) {
        return quotationService.listByOpportunityId(opportunityId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/{id}/versions")
    @Operation(summary = "获取报价单版本列表")
    public List<QuotationVO> listVersions(@PathVariable Long id) {
        Quotation quotation = quotationService.getById(id);
        // 缺失空值守卫时 NPE 会被兜底成 500（2026-09-23 真机实测），应报 404
        if (quotation == null) {
            throw BusinessException.notFound("报价单不存在");
        }
        Long parentId = quotation.getParentId() != null ? quotation.getParentId() : quotation.getId();
        return quotationService.listVersions(parentId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping
    @Operation(summary = "创建报价单")
    public QuotationVO create(@RequestBody QuotationCreateDTO dto) {
        Quotation quotation = new Quotation();
        BeanUtils.copyProperties(dto, quotation);
        // tenantId 交由 MetaObjectHandler 从会话租户填充，不再硬编码 1（CRM-BREAK-02 同类）
        quotation.setCreateBy(StpUtil.getLoginIdAsLong());
        List<QuotationItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                QuotationItem item = new QuotationItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Quotation created = quotationService.createQuotation(quotation, items);
        return convertToVO(created);
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping("/from-opportunity/{opportunityId}")
    @Operation(summary = "从商机创建报价单")
    public QuotationVO createFromOpportunity(@PathVariable Long opportunityId) {
        Quotation quotation = quotationService.createFromOpportunity(opportunityId);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping("/from-template/{templateId}")
    @Operation(summary = "从模板创建报价单")
    public QuotationVO createFromTemplate(
            @PathVariable Long templateId,
            @RequestParam Long customerId) {
        Quotation quotation = quotationService.createFromTemplate(templateId, customerId);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制报价单")
    public QuotationVO copy(@PathVariable Long id) {
        Quotation quotation = quotationService.copyQuotation(id);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping("/{id}/new-version")
    @Operation(summary = "创建新版本")
    public QuotationVO createNewVersion(@PathVariable Long id) {
        Quotation quotation = quotationService.createNewVersion(id);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PutMapping("/{id}")
    @Operation(summary = "更新报价单")
    public QuotationVO update(@PathVariable Long id, @RequestBody QuotationCreateDTO dto) {
        Quotation quotation = new Quotation();
        BeanUtils.copyProperties(dto, quotation);
        List<QuotationItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                QuotationItem item = new QuotationItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        Quotation updated = quotationService.updateQuotation(id, quotation, items);
        return convertToVO(updated);
    }

    @SaCheckPermission("crm:quotation:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public QuotationVO submitForApproval(@PathVariable Long id) {
        Quotation quotation = quotationService.submitForApproval(id);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public QuotationVO approve(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Quotation quotation = quotationService.approve(id, approverId, note);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public QuotationVO reject(
            @PathVariable Long id,
            @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        Quotation quotation = quotationService.reject(id, rejecterId, reason);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:send")
    @PostMapping("/{id}/send")
    @Operation(summary = "发送给客户")
    public QuotationVO sendToCustomer(
            @PathVariable Long id,
            @RequestParam String method) {
        Long senderId = StpUtil.getLoginIdAsLong();
        Quotation quotation = quotationService.sendToCustomer(id, senderId, method);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PostMapping("/{id}/accept")
    @Operation(summary = "标记为已接受")
    public QuotationVO markAccepted(
            @PathVariable Long id,
            @RequestParam(required = false) String note) {
        Long accepterId = StpUtil.getLoginIdAsLong();
        Quotation quotation = quotationService.markAccepted(id, accepterId, note);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PostMapping("/{id}/reject-by-customer")
    @Operation(summary = "标记为已拒绝")
    public QuotationVO markRejected(
            @PathVariable Long id,
            @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        Quotation quotation = quotationService.markRejected(id, rejecterId, reason);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:convert")
    @PostMapping("/{id}/convert")
    @Operation(summary = "转销售订单")
    public QuotationVO convertToOrder(@PathVariable Long id) {
        Quotation quotation = quotationService.convertToOrder(id);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消报价单")
    public QuotationVO cancel(
            @PathVariable Long id,
            @RequestParam String reason) {
        Quotation quotation = quotationService.cancel(id, reason);
        return convertToVO(quotation);
    }

    @SaCheckPermission("crm:quotation:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除报价单")
    public boolean delete(@PathVariable Long id) {
        return quotationService.removeById(id);
    }

    @SaCheckPermission("crm:quotation:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除报价单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return quotationService.removeBatchByIds(ids);
    }

    @SaCheckPermission("crm:quotation:downloadpdf")
    @GetMapping("/export")
    @Operation(summary = "导出报价单列表")
    public List<Quotation> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机ID") @RequestParam(required = false) Long opportunityId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售员ID") @RequestParam(required = false) Long salesPersonId) {
        return quotationService.exportList(keyword, customerId, opportunityId, status, salesPersonId);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加报价明细")
    public QuotationItem addItem(@PathVariable Long id, @RequestBody QuotationItemDTO dto) {
        QuotationItem item = new QuotationItem();
        BeanUtils.copyProperties(dto, item);
        return quotationService.addItem(id, item);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新报价明细")
    public QuotationItem updateItem(@PathVariable Long itemId, @RequestBody QuotationItemDTO dto) {
        QuotationItem item = new QuotationItem();
        BeanUtils.copyProperties(dto, item);
        return quotationService.updateItem(itemId, item);
    }

    @SaCheckPermission("crm:quotation:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除报价明细")
    public void removeItem(@PathVariable Long itemId) {
        quotationService.removeItem(itemId);
    }

    @SaCheckPermission("crm:quotation:edit")
    @PostMapping("/{id}/items/reorder")
    @Operation(summary = "调整明细顺序")
    public void reorderItems(@PathVariable Long id, @RequestBody List<Long> itemIds) {
        quotationService.reorderItems(id, itemIds);
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/expired")
    @Operation(summary = "获取过期报价单")
    public List<QuotationVO> getExpiredQuotations() {
        return quotationService.getExpiredQuotations().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("crm:quotation:create")
    @PostMapping("/mark-expired")
    @Operation(summary = "标记过期报价单")
    public void markExpiredQuotations() {
        quotationService.markExpiredQuotations();
    }

    @SaCheckPermission("crm:quotation:view")
    @GetMapping("/statistics")
    @Operation(summary = "报价统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (QuotationStatus status : QuotationStatus.values()) {
            stats.put(status.getDesc(), quotationService.lambdaQuery()
                    .eq(Quotation::getStatus, status.getCode())
                    .eq(Quotation::getDeleted, 0)
                    .count());
        }
        return stats;
    }

    private QuotationVO convertToVO(Quotation quotation) {
        QuotationVO vo = new QuotationVO();
        BeanUtils.copyProperties(quotation, vo);
        for (QuotationStatus status : QuotationStatus.values()) {
            if (status.getCode().equals(quotation.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        if (quotation.getValidTo() != null) {
            vo.setIsExpired(quotation.getValidTo().isBefore(java.time.LocalDate.now()));
        }
        return vo;
    }
}