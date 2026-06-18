package cn.aiedge.crm.quotation.service;

import cn.aiedge.crm.quotation.entity.QuotationTemplate;
import cn.aiedge.crm.quotation.entity.QuotationTemplateItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface QuotationTemplateService extends IService<QuotationTemplate> {

    QuotationTemplate getByTemplateCode(String templateCode);

    Page<QuotationTemplate> pageList(String keyword, Integer templateType, Long customerId, int pageNum, int pageSize);

    List<QuotationTemplate> listActiveTemplates();

    List<QuotationTemplate> listByCustomerId(Long customerId);

    List<QuotationTemplate> listByCategoryId(Long categoryId);

    String generateTemplateCode();

    QuotationTemplate createTemplate(QuotationTemplate template, List<QuotationTemplateItem> items);

    QuotationTemplate updateTemplate(Long templateId, QuotationTemplate template, List<QuotationTemplateItem> items);

    QuotationTemplate copyTemplate(Long templateId);

    void activateTemplate(Long templateId);

    void deactivateTemplate(Long templateId);

    void incrementUsageCount(Long templateId);

    List<QuotationTemplateItem> getTemplateItems(Long templateId);

    QuotationTemplateItem addTemplateItem(Long templateId, QuotationTemplateItem item);

    QuotationTemplateItem updateTemplateItem(Long itemId, QuotationTemplateItem item);

    void removeTemplateItem(Long itemId);
}