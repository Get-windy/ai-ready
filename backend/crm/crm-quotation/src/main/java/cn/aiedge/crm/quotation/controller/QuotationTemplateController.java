package cn.aiedge.crm.quotation.controller;

import cn.aiedge.crm.quotation.entity.QuotationTemplate;
import cn.aiedge.crm.quotation.entity.QuotationTemplateItem;
import cn.aiedge.crm.quotation.service.QuotationTemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/crm/quotation-template")
@RequiredArgsConstructor
@Tag(name = "报价模板管理", description = "报价模板的创建、管理、使用")
public class QuotationTemplateController {

    private final QuotationTemplateService templateService;

    @GetMapping("/page")
    @Operation(summary = "分页查询报价模板")
    public Page<QuotationTemplate> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "模板类型") @RequestParam(required = false) Integer templateType,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return templateService.pageList(keyword, templateType, customerId, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public QuotationTemplate getById(@PathVariable Long id) {
        QuotationTemplate template = templateService.getById(id);
        if (template == null) {
            throw new RuntimeException("报价模板不存在");
        }
        return template;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取模板明细")
    public List<QuotationTemplateItem> getTemplateItems(@PathVariable Long id) {
        return templateService.getTemplateItems(id);
    }

    @GetMapping("/active")
    @Operation(summary = "获取活跃模板列表")
    public List<QuotationTemplate> listActiveTemplates() {
        return templateService.listActiveTemplates();
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的模板列表")
    public List<QuotationTemplate> listByCustomerId(@PathVariable Long customerId) {
        return templateService.listByCustomerId(customerId);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "获取产品分类的模板列表")
    public List<QuotationTemplate> listByCategoryId(@PathVariable Long categoryId) {
        return templateService.listByCategoryId(categoryId);
    }

    @PostMapping
    @Operation(summary = "创建报价模板")
    public QuotationTemplate create(@RequestBody QuotationTemplate template) {
        template.setTenantId(1L);
        return templateService.createTemplate(template, null);
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制报价模板")
    public QuotationTemplate copy(@PathVariable Long id) {
        return templateService.copyTemplate(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报价模板")
    public QuotationTemplate update(@PathVariable Long id, @RequestBody QuotationTemplate template) {
        return templateService.updateTemplate(id, template, null);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活模板")
    public void activate(@PathVariable Long id) {
        templateService.activateTemplate(id);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用模板")
    public void deactivate(@PathVariable Long id) {
        templateService.deactivateTemplate(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报价模板")
    public void delete(@PathVariable Long id) {
        templateService.removeById(id);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加模板明细")
    public QuotationTemplateItem addTemplateItem(@PathVariable Long id, @RequestBody QuotationTemplateItem item) {
        return templateService.addTemplateItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新模板明细")
    public QuotationTemplateItem updateTemplateItem(@PathVariable Long itemId, @RequestBody QuotationTemplateItem item) {
        return templateService.updateTemplateItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除模板明细")
    public void removeTemplateItem(@PathVariable Long itemId) {
        templateService.removeTemplateItem(itemId);
    }
}