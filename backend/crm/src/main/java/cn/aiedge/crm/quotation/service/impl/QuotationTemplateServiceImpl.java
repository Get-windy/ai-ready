package cn.aiedge.crm.quotation.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.quotation.entity.QuotationTemplate;
import cn.aiedge.crm.quotation.entity.QuotationTemplateItem;
import cn.aiedge.crm.quotation.mapper.QuotationTemplateItemMapper;
import cn.aiedge.crm.quotation.mapper.QuotationTemplateMapper;
import cn.aiedge.crm.quotation.service.QuotationTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationTemplateServiceImpl extends ServiceImpl<QuotationTemplateMapper, QuotationTemplate> implements QuotationTemplateService {

    private final QuotationTemplateItemMapper templateItemMapper;

    @Override
    public QuotationTemplate getByTemplateCode(String templateCode) {
        return lambdaQuery()
                .eq(QuotationTemplate::getTemplateCode, templateCode)
                .eq(QuotationTemplate::getDeleted, 0)
                .one();
    }

    @Override
    public Page<QuotationTemplate> pageList(String keyword, Integer templateType, Long customerId, int pageNum, int pageSize) {
        LambdaQueryWrapper<QuotationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuotationTemplate::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(QuotationTemplate::getTemplateCode, keyword)
                    .or().like(QuotationTemplate::getTemplateName, keyword));
        }
        if (templateType != null) {
            wrapper.eq(QuotationTemplate::getTemplateType, templateType);
        }
        if (customerId != null) {
            wrapper.eq(QuotationTemplate::getCustomerId, customerId);
        }
        wrapper.orderByDesc(QuotationTemplate::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<QuotationTemplate> listActiveTemplates() {
        return baseMapper.selectActiveTemplates();
    }

    @Override
    public List<QuotationTemplate> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<QuotationTemplate> listByCategoryId(Long categoryId) {
        return baseMapper.selectByCategoryId(categoryId);
    }

    @Override
    public String generateTemplateCode() {
        String prefix = "QT-TPL";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<QuotationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(QuotationTemplate::getTemplateCode, prefix + dateStr)
                .eq(QuotationTemplate::getDeleted, 0)
                .orderByDesc(QuotationTemplate::getTemplateCode)
                .last("LIMIT 1");
        QuotationTemplate lastTemplate = getOne(wrapper);
        int seq = 1;
        if (lastTemplate != null) {
            String lastNo = lastTemplate.getTemplateCode();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplate createTemplate(QuotationTemplate template, List<QuotationTemplateItem> items) {
        template.setTemplateCode(generateTemplateCode());
        template.setActive(true);
        template.setUsageCount(0);
        save(template);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                QuotationTemplateItem item = items.get(i);
                item.setTemplateId(template.getId());
                item.setLineNo(i + 1);
                item.setTenantId(template.getTenantId());
                templateItemMapper.insert(item);
            }
        }
        return getById(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplate updateTemplate(Long templateId, QuotationTemplate template, List<QuotationTemplateItem> items) {
        QuotationTemplate existing = getById(templateId);
        if (existing == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        template.setId(templateId);
        updateById(template);
        if (items != null) {
            List<QuotationTemplateItem> existingItems = getTemplateItems(templateId);
            for (QuotationTemplateItem oldItem : existingItems) {
                templateItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                QuotationTemplateItem item = items.get(i);
                item.setTemplateId(templateId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                templateItemMapper.insert(item);
            }
        }
        return getById(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplate copyTemplate(Long templateId) {
        QuotationTemplate source = getById(templateId);
        if (source == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        QuotationTemplate copy = new QuotationTemplate();
        copy.setTemplateName(source.getTemplateName() + "(副本)");
        copy.setDescription(source.getDescription());
        copy.setTemplateType(source.getTemplateType());
        copy.setCustomerId(source.getCustomerId());
        copy.setCustomerName(source.getCustomerName());
        copy.setProductCategoryId(source.getProductCategoryId());
        copy.setCategoryName(source.getCategoryName());
        copy.setDefaultDiscountRate(source.getDefaultDiscountRate());
        copy.setDefaultTaxRate(source.getDefaultTaxRate());
        copy.setPaymentTerms(source.getPaymentTerms());
        copy.setPaymentDays(source.getPaymentDays());
        copy.setDeliveryTerms(source.getDeliveryTerms());
        copy.setDeliveryDays(source.getDeliveryDays());
        copy.setTermsAndConditions(source.getTermsAndConditions());
        copy.setFooterNote(source.getFooterNote());
        List<QuotationTemplateItem> sourceItems = getTemplateItems(templateId);
        return createTemplate(copy, sourceItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateTemplate(Long templateId) {
        QuotationTemplate template = getById(templateId);
        if (template == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        template.setActive(true);
        updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateTemplate(Long templateId) {
        QuotationTemplate template = getById(templateId);
        if (template == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        template.setActive(false);
        updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long templateId) {
        QuotationTemplate template = getById(templateId);
        if (template == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsedTime(java.time.LocalDateTime.now());
        updateById(template);
    }

    @Override
    public List<QuotationTemplateItem> getTemplateItems(Long templateId) {
        return templateItemMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplateItem addTemplateItem(Long templateId, QuotationTemplateItem item) {
        QuotationTemplate template = getById(templateId);
        if (template == null) {
            throw BusinessException.notFound("报价模板不存在");
        }
        List<QuotationTemplateItem> existingItems = getTemplateItems(templateId);
        item.setTemplateId(templateId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(template.getTenantId());
        templateItemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationTemplateItem updateTemplateItem(Long itemId, QuotationTemplateItem item) {
        QuotationTemplateItem existing = templateItemMapper.selectById(itemId);
        if (existing == null) {
            throw BusinessException.notFound("模板明细不存在");
        }
        item.setId(itemId);
        templateItemMapper.updateById(item);
        return templateItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTemplateItem(Long itemId) {
        templateItemMapper.deleteById(itemId);
    }
}