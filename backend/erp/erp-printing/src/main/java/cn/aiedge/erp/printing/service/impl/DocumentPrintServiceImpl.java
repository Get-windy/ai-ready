package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.dto.v2.DocumentPrintResult;
import cn.aiedge.erp.printing.engine.FormatEngine;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;
import cn.aiedge.erp.printing.entity.SetPrintConfig;
import cn.aiedge.erp.printing.mapper.SysPrintTemplateMapper;
import cn.aiedge.erp.printing.service.DocumentPrintService;
import cn.aiedge.erp.printing.service.PrintConfigService;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.spi.PrintDataProviderRegistry;
import cn.aiedge.erp.printing.support.PrintBehaviorApplier;
import cn.aiedge.erp.printing.support.PrintTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单据打印实现：装配器取数 → 解析模板 → 渲染 HTML。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPrintServiceImpl implements DocumentPrintService {

    /** 模板只有发布态才对打印可见；草稿是设计器里的半成品 */
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final PrintDataProviderRegistry providerRegistry;
    private final SysPrintTemplateMapper templateMapper;
    private final PrintConfigService printConfigService;
    private final FormatEngine formatEngine;
    private final ObjectMapper objectMapper;

    @Override
    public Set<String> supportedPageCodes() {
        return providerRegistry.pageCodes();
    }

    @Override
    public boolean supports(String pageCode) {
        return providerRegistry.supports(pageCode);
    }

    @Override
    public List<SysPrintTemplate> listPublished(String pageCode) {
        LambdaQueryWrapper<SysPrintTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTemplate::getTenantId, PrintTenantSupport.currentTenantId())
                .eq(SysPrintTemplate::getPageCode, pageCode)
                .eq(SysPrintTemplate::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SysPrintTemplate::getIsDefault)
                .orderByDesc(SysPrintTemplate::getUpdatedAt);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public DocumentPrintResult render(String pageCode, Long documentId, Long templateId) {
        if (documentId == null) {
            throw BusinessException.badRequest("缺少单据主键，无法打印");
        }
        PrintDataProvider provider = providerRegistry.require(pageCode);

        Map<String, Object> data = provider.load(documentId);
        if (data == null || data.isEmpty()) {
            throw BusinessException.notFound("单据不存在或没有可打印内容：" + pageCode + " #" + documentId);
        }

        // 「打印设置」（小数位数 / 打印内容）必须在服务端生效：
        // 数据是服务端装配的，前端那份 applyPrintBehavior 拿不到它。
        Long tenantId = PrintTenantSupport.currentTenantId();
        SetPrintConfig config = printConfigService.getOrCreate(tenantId, PrintTenantSupport.currentUserId());
        PrintBehaviorApplier.apply(data, config);

        List<SysPrintTemplate> published = listPublished(pageCode);
        SysPrintTemplate template = pickTemplate(published, templateId, pageCode);

        String html = formatEngine.renderToHtml(template.getTemplateJson(), toJson(data));

        DocumentPrintResult result = new DocumentPrintResult();
        result.setHtml(html);
        result.setTemplateId(template.getTemplateId());
        result.setTemplateName(template.getTemplateName());
        result.setPaperSize(template.getPaperSize());
        result.setPaperWidth(template.getPaperWidth());
        result.setPaperHeight(template.getPaperHeight());
        result.setDefaultTemplateId(defaultTemplateId(published));
        result.setDocumentNo(provider.documentNo(documentId));
        return result;
    }

    /**
     * 挑模板：
     * ① 调用方指定了 id → 必须命中该页面已发布的模板（防止拿 A 页面的模板打 B 页面的单据）
     * ② 没指定 → 默认模板（is_default）→ 最近发布的一条
     */
    private SysPrintTemplate pickTemplate(List<SysPrintTemplate> published, Long templateId, String pageCode) {
        if (published.isEmpty()) {
            throw BusinessException.badRequest("页面「" + pageCode + "」还没有已发布的打印模板，"
                    + "请先在「打印模板」中创建并发布（草稿状态不参与打印）");
        }
        if (templateId != null) {
            for (SysPrintTemplate candidate : published) {
                if (templateId.equals(candidate.getTemplateId())) {
                    return candidate;
                }
            }
            throw BusinessException.badRequest("模板 " + templateId + " 不属于页面「" + pageCode
                    + "」或尚未发布");
        }
        return published.get(0);
    }

    private Long defaultTemplateId(List<SysPrintTemplate> published) {
        for (SysPrintTemplate template : published) {
            if (Boolean.TRUE.equals(template.getIsDefault())) {
                return template.getTemplateId();
            }
        }
        return published.isEmpty() ? null : published.get(0).getTemplateId();
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("打印数据序列化失败: {}", e.getMessage(), e);
            throw BusinessException.internalError("打印数据序列化失败：" + e.getMessage());
        }
    }
}
