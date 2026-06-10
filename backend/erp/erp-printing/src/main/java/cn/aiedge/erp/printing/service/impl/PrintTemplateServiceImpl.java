package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.entity.PrintTemplate;
import cn.aiedge.erp.printing.mapper.PrintTemplateMapper;
import cn.aiedge.erp.printing.service.PrintTemplateService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrintTemplateServiceImpl implements PrintTemplateService {

    private final PrintTemplateMapper templateMapper;

    @Override
    @Transactional
    public PrintTemplate createTemplate(PrintTemplate template) {
        template.setTemplateCode("TPL" + IdUtil.fastSimpleUUID().substring(0, 8));
        template.setStatus(1);
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public PrintTemplate updateTemplate(Long id, PrintTemplate template) {
        PrintTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("模板不存在");
        }
        template.setId(id);
        templateMapper.updateById(template);
        return templateMapper.selectById(id);
    }

    @Override
    public PrintTemplate getTemplateById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public Page<PrintTemplate> listTemplates(Integer page, Integer size, String templateType, String status) {
        Page<PrintTemplate> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateType != null && !templateType.isEmpty()) {
            wrapper.eq(PrintTemplate::getTemplateType, templateType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PrintTemplate::getStatus, Integer.parseInt(status));
        }
        wrapper.orderByDesc(PrintTemplate::getCreateTime);
        return templateMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    @Transactional
    public PrintTemplate copyTemplate(Long id, String newName) {
        PrintTemplate source = templateMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("模板不存在");
        }
        PrintTemplate copy = new PrintTemplate();
        copy.setTemplateCode("TPL" + IdUtil.fastSimpleUUID().substring(0, 8));
        copy.setTemplateName(newName);
        copy.setTemplateType(source.getTemplateType());
        copy.setFileType(source.getFileType());
        copy.setFilePath(source.getFilePath());
        copy.setContent(source.getContent());
        copy.setVariables(source.getVariables());
        copy.setWidth(source.getWidth());
        copy.setHeight(source.getHeight());
        copy.setOrientation(source.getOrientation());
        copy.setCopies(source.getCopies());
        copy.setPaperSize(source.getPaperSize());
        copy.setStatus(1);
        copy.setRemark("复制自: " + source.getTemplateName());
        templateMapper.insert(copy);
        return copy;
    }

    @Override
    public String previewTemplate(Long id, String printData) {
        PrintTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.notFound("模板不存在");
        }
        return template.getContent();
    }

    @Override
    public List<PrintTemplate> listByType(String templateType) {
        LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintTemplate::getTemplateType, templateType)
               .eq(PrintTemplate::getStatus, 1)
               .orderByAsc(PrintTemplate::getTemplateName);
        return templateMapper.selectList(wrapper);
    }
}