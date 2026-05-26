package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ReportTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ReportTemplateService extends IService<ReportTemplate> {
    
    List<ReportTemplate> listByReportType(Long tenantId, Integer reportType);
    
    ReportTemplate getByCode(Long tenantId, String templateCode);
    
    Page<ReportTemplate> pageList(Long tenantId, Integer reportType, String templateName, Integer enabled, Page<ReportTemplate> page);
    
    boolean createTemplate(ReportTemplate template);
    
    boolean updateTemplate(ReportTemplate template);
    
    boolean deleteTemplate(Long tenantId, Long templateId);
    
    boolean initStandardTemplates(Long tenantId, Integer reportType);
}