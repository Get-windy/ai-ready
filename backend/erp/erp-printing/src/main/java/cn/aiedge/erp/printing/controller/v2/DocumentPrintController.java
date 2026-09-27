package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.DocumentPrintResult;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;
import cn.aiedge.erp.printing.service.DocumentPrintService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据打印（业务级接口）。
 *
 * 页面只给 pageCode + 单据主键，取数/挑模板/渲染都在后端；
 * 这是「打印做成系统级组件」的对外契约，新页面接入不需要写任何渲染代码。
 *
 * 与 {@code /api/v2/print/format/render} 的分工：
 *   · 本接口是**业务级**的，页面用；
 *   · format/render 是**开发者级**的，接收两个字符串，供设计器预览与调试。
 */
@Tag(name = "V2-单据打印", description = "按页面编码 + 单据主键渲染打印内容，页面无需关心模板与取数")
@RestController
@RequestMapping("/api/v2/print/documents")
@RequiredArgsConstructor
public class DocumentPrintController {

    private final DocumentPrintService documentPrintService;

    @Operation(summary = "已注册数据装配器的页面编码（接入检查用）")
    @SaCheckPermission("print:template:list")
    @GetMapping("/page-codes")
    public ResponseEntity<ApiResponse<Object>> pageCodes() {
        return ResponseEntity.ok(ApiResponse.ok(documentPrintService.supportedPageCodes()));
    }

    @Operation(summary = "页面已发布模板列表（含是否后端可装配数据）")
    @SaCheckPermission("print:template:list")
    @GetMapping("/{pageCode}/templates")
    public ResponseEntity<ApiResponse<Object>> templates(@PathVariable String pageCode) {
        List<SysPrintTemplate> published = documentPrintService.listPublished(pageCode);
        List<Map<String, Object>> templates = new ArrayList<>();
        Long defaultTemplateId = null;
        for (SysPrintTemplate template : published) {
            // 连 templateJson 一起下发：打印弹窗拿到模板后要立刻渲染（兼容路径），
            // 若改成「选中后再单独取详情」，权限面会从 print:template:list 收窄到
            // print:template:detail —— 只有 list 的角色会突然打不出预览。
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("templateId", template.getTemplateId());
            item.put("templateName", template.getTemplateName());
            item.put("isDefault", Boolean.TRUE.equals(template.getIsDefault()));
            item.put("paperSize", template.getPaperSize());
            item.put("version", template.getVersion());
            item.put("updatedAt", template.getUpdatedAt());
            item.put("templateJson", template.getTemplateJson());
            templates.add(item);
            if (defaultTemplateId == null && Boolean.TRUE.equals(template.getIsDefault())) {
                defaultTemplateId = template.getTemplateId();
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageCode", pageCode);
        // supported=false 表示该页面还没实现 PrintDataProvider，只能走「页面自己给数据」的老路
        body.put("supported", documentPrintService.supports(pageCode));
        body.put("defaultTemplateId", defaultTemplateId != null ? defaultTemplateId
                : (templates.isEmpty() ? null : templates.get(0).get("templateId")));
        body.put("templates", templates);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @Operation(summary = "按单据渲染打印内容")
    @SaCheckPermission("print:format:view")
    @PostMapping("/{pageCode}/{documentId}/render")
    public ResponseEntity<ApiResponse<Object>> render(
            @PathVariable String pageCode,
            @PathVariable Long documentId,
            @RequestParam(required = false) Long templateId) {
        DocumentPrintResult result = documentPrintService.render(pageCode, documentId, templateId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
