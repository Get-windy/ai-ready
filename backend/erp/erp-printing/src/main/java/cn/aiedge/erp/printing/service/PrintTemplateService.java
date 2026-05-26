package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.PrintTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PrintTemplateService {

    PrintTemplate createTemplate(PrintTemplate template);

    PrintTemplate updateTemplate(Long id, PrintTemplate template);

    PrintTemplate getTemplateById(Long id);

    Page<PrintTemplate> listTemplates(Integer page, Integer size, String templateType, String status);

    void deleteTemplate(Long id);

    PrintTemplate copyTemplate(Long id, String newName);

    String previewTemplate(Long id, String printData);

    List<PrintTemplate> listByType(String templateType);
}