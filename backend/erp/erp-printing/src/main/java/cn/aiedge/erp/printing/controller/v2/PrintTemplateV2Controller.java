package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.PrintTemplateCreateRequest;
import cn.aiedge.erp.printing.dto.v2.PrintTemplateVO;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;
import cn.aiedge.erp.printing.mapper.SysPrintTemplateMapper;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "V2-打印模板管理", description = "可视化设计器存储的模板管理，含 template_json 结构")
@RestController
@RequestMapping("/api/v2/print/templates")
@RequiredArgsConstructor
public class PrintTemplateV2Controller {

    private final SysPrintTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /**
     * 从 Sa-Token 会话或请求头获取 tenantId
     */
    private Long resolveTenantId(Long headerTenantId) {
        if (headerTenantId != null) {
            return headerTenantId;
        }
        try {
            Object sessionTenantId = StpUtil.getSession().get("tenantId");
            if (sessionTenantId instanceof Number) {
                return ((Number) sessionTenantId).longValue();
            }
        } catch (Exception e) {
            log.warn("无法从Sa-Token会话获取tenantId: {}", e.getMessage());
        }
        return 1L; // 默认租户
    }

    /**
     * 从 Sa-Token 会话获取 userId
     */
    private Long resolveUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Operation(summary = "创建模板")
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Object>> create(
            @Valid @RequestBody PrintTemplateCreateRequest request,
            @RequestHeader(required = false) Long tenantId,
            @RequestHeader(required = false) Long userId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Long resolvedUserId = resolveUserId(userId);
        // 校验重名
        LambdaQueryWrapper<SysPrintTemplate> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintTemplate::getTenantId, resolvedTenantId)
                .eq(SysPrintTemplate::getPageCode, request.getPageCode())
                .eq(SysPrintTemplate::getTemplateName, request.getTemplateName());
        if (templateMapper.selectCount(nameCheck) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板名称已存在: " + request.getTemplateName()));
        }

        SysPrintTemplate entity = new SysPrintTemplate();
        entity.setTenantId(resolvedTenantId);
        entity.setPageCode(request.getPageCode());
        entity.setTemplateName(request.getTemplateName());
        entity.setTemplateJson(request.getTemplateJson() != null ? request.getTemplateJson() : "{}");
        entity.setPaperSize(request.getPaperSize() != null ? request.getPaperSize() : "A4");
        entity.setPaperWidth(request.getPaperWidth());
        entity.setPaperHeight(request.getPaperHeight());
        entity.setMarginTop(request.getMarginTop());
        entity.setMarginBottom(request.getMarginBottom());
        entity.setMarginLeft(request.getMarginLeft());
        entity.setMarginRight(request.getMarginRight());
        entity.setStatus("DRAFT");
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        entity.setVersion(1);
        entity.setCreatedBy(resolvedUserId);
        templateMapper.insert(entity);

        return ResponseEntity.ok(ApiResponse.ok(toVO(entity)));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> update(
            @PathVariable Long id,
            @RequestBody PrintTemplateCreateRequest request,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        SysPrintTemplate entity = templateMapper.selectById(id);
        if (entity == null || !entity.getTenantId().equals(resolvedTenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板不存在"));
        }

        // 校验重名（排除自身）
        LambdaQueryWrapper<SysPrintTemplate> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(SysPrintTemplate::getTenantId, resolvedTenantId)
                .eq(SysPrintTemplate::getPageCode, request.getPageCode())
                .eq(SysPrintTemplate::getTemplateName, request.getTemplateName())
                .ne(SysPrintTemplate::getTemplateId, id);
        if (templateMapper.selectCount(nameCheck) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板名称已存在: " + request.getTemplateName()));
        }

        if (StrUtil.isNotBlank(request.getPageCode())) entity.setPageCode(request.getPageCode());
        if (StrUtil.isNotBlank(request.getTemplateName())) entity.setTemplateName(request.getTemplateName());
        if (request.getTemplateJson() != null) entity.setTemplateJson(request.getTemplateJson());
        if (request.getPaperSize() != null) entity.setPaperSize(request.getPaperSize());
        entity.setPaperWidth(request.getPaperWidth());
        entity.setPaperHeight(request.getPaperHeight());
        entity.setMarginTop(request.getMarginTop());
        entity.setMarginBottom(request.getMarginBottom());
        entity.setMarginLeft(request.getMarginLeft());
        entity.setMarginRight(request.getMarginRight());
        entity.setVersion(entity.getVersion() + 1);
        templateMapper.updateById(entity);

        return ResponseEntity.ok(ApiResponse.ok(toVO(templateMapper.selectById(id))));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        SysPrintTemplate entity = templateMapper.selectById(id);
        if (entity == null || !entity.getTenantId().equals(resolvedTenantId)) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return ResponseEntity.ok(ApiResponse.ok(toVO(entity)));
    }

    @Operation(summary = "模板列表")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String pageCode,
            @RequestParam(required = false) String status,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Page<SysPrintTemplate> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysPrintTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTemplate::getTenantId, resolvedTenantId);
        if (StrUtil.isNotBlank(pageCode)) {
            wrapper.eq(SysPrintTemplate::getPageCode, pageCode);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(SysPrintTemplate::getStatus, status);
        }
        wrapper.orderByDesc(SysPrintTemplate::getCreatedAt);

        Page<SysPrintTemplate> resultPage = templateMapper.selectPage(pageObj, wrapper);
        return ResponseEntity.ok(ApiResponse.ok(resultPage));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        SysPrintTemplate entity = templateMapper.selectById(id);
        if (entity == null || !entity.getTenantId().equals(resolvedTenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板不存在"));
        }
        templateMapper.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "发布模板")
    @PutMapping("/{id}/publish")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> publish(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        SysPrintTemplate entity = templateMapper.selectById(id);
        if (entity == null || !entity.getTenantId().equals(resolvedTenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板不存在"));
        }
        entity.setStatus("PUBLISHED");
        templateMapper.updateById(entity);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "复制模板")
    @PostMapping("/{id}/copy")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> copy(
            @PathVariable Long id,
            @RequestParam String newName,
            @RequestHeader(required = false) Long tenantId,
            @RequestHeader(required = false) Long userId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Long resolvedUserId = resolveUserId(userId);
        SysPrintTemplate source = templateMapper.selectById(id);
        if (source == null || !source.getTenantId().equals(resolvedTenantId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("模板不存在"));
        }

        SysPrintTemplate copy = new SysPrintTemplate();
        copy.setTenantId(resolvedTenantId);
        copy.setPageCode(source.getPageCode());
        copy.setTemplateName(newName);
        copy.setTemplateJson(source.getTemplateJson());
        copy.setPaperSize(source.getPaperSize());
        copy.setPaperWidth(source.getPaperWidth());
        copy.setPaperHeight(source.getPaperHeight());
        copy.setMarginTop(source.getMarginTop());
        copy.setMarginBottom(source.getMarginBottom());
        copy.setMarginLeft(source.getMarginLeft());
        copy.setMarginRight(source.getMarginRight());
        copy.setStatus("DRAFT");
        copy.setVersion(1);
        copy.setCreatedBy(resolvedUserId);
        templateMapper.insert(copy);

        return ResponseEntity.ok(ApiResponse.ok(toVO(copy)));
    }

    private PrintTemplateVO toVO(SysPrintTemplate entity) {
        PrintTemplateVO vo = new PrintTemplateVO();
        vo.setTemplateId(entity.getTemplateId());
        vo.setTenantId(entity.getTenantId());
        vo.setPageCode(entity.getPageCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setPaperSize(entity.getPaperSize());
        vo.setPaperWidth(entity.getPaperWidth());
        vo.setPaperHeight(entity.getPaperHeight());
        vo.setMarginTop(entity.getMarginTop());
        vo.setMarginBottom(entity.getMarginBottom());
        vo.setMarginLeft(entity.getMarginLeft());
        vo.setMarginRight(entity.getMarginRight());
        vo.setStatus(entity.getStatus());
        vo.setIsDefault(entity.getIsDefault());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        // 解析 template_json
        try {
            if (StrUtil.isNotBlank(entity.getTemplateJson())) {
                Map<String, Object> json = objectMapper.readValue(
                        entity.getTemplateJson(),
                        new TypeReference<Map<String, Object>>() {});
                vo.setTemplateJson(json);
            }
        } catch (Exception e) {
            log.warn("解析templateJson失败, templateId={}", entity.getTemplateId(), e);
        }

        return vo;
    }
}
