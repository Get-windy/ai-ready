package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.v2.DocumentPrintResult;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;

import java.util.List;
import java.util.Set;

/**
 * 单据打印（业务级）。
 *
 * 页面只需要给「我是哪个页面 + 单据主键」，取数、挑模板、渲染全在这里完成；
 * 页面不必再懂 templateJson、也不必自己维护模板优先级。
 */
public interface DocumentPrintService {

    /** 后端已注册数据装配器的页面编码（其余页面只能走「页面自己给数据」的兼容模式） */
    Set<String> supportedPageCodes();

    boolean supports(String pageCode);

    /** 某页面已发布的模板，默认模板排在最前 */
    List<SysPrintTemplate> listPublished(String pageCode);

    /**
     * 按单据渲染打印 HTML。
     *
     * @param templateId 指定模板；传 null 时用该页面的默认模板（没有默认就取最近发布的）
     */
    DocumentPrintResult render(String pageCode, Long documentId, Long templateId);
}
