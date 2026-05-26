package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.ReportTemplate;
import cn.aiedge.finance.enums.ReportType;
import cn.aiedge.finance.mapper.ReportTemplateMapper;
import cn.aiedge.finance.service.ReportTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements ReportTemplateService {
    
    @Override
    public List<ReportTemplate> listByReportType(Long tenantId, Integer reportType) {
        return baseMapper.listByReportType(tenantId, reportType);
    }
    
    @Override
    public ReportTemplate getByCode(Long tenantId, String templateCode) {
        return baseMapper.getByCode(tenantId, templateCode);
    }
    
    @Override
    public Page<ReportTemplate> pageList(Long tenantId, Integer reportType, String templateName, Integer enabled, Page<ReportTemplate> page) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getTenantId, tenantId)
               .eq(ReportTemplate::getDeleted, 0);
        if (reportType != null) {
            wrapper.eq(ReportTemplate::getReportType, reportType);
        }
        if (templateName != null && !templateName.isEmpty()) {
            wrapper.like(ReportTemplate::getTemplateName, templateName);
        }
        if (enabled != null) {
            wrapper.eq(ReportTemplate::getEnabled, enabled);
        }
        wrapper.orderByAsc(ReportTemplate::getDisplayOrder);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTemplate(ReportTemplate template) {
        ReportTemplate existing = this.getByCode(template.getTenantId(), template.getTemplateCode());
        if (existing != null) {
            throw new RuntimeException("模板编码已存在");
        }
        template.setEnabled(1);
        return this.save(template);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(ReportTemplate template) {
        return this.updateById(template);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long tenantId, Long templateId) {
        return this.removeById(templateId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initStandardTemplates(Long tenantId, Integer reportType) {
        List<ReportTemplate> templates = new ArrayList<>();
        if (reportType == ReportType.BALANCE_SHEET.getCode()) {
            templates.add(createTemplateRow(tenantId, "BS001", "流动资产", 1, 1, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS002", "货币资金", 1, 2, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS003", "交易性金融资产", 1, 3, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS004", "应收票据", 1, 4, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS005", "应收账款", 1, 5, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS006", "预付款项", 1, 6, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS007", "其他应收款", 1, 7, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS008", "存货", 1, 8, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS009", "流动资产合计", 1, 9, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS010", "非流动资产", 1, 10, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS011", "长期股权投资", 1, 11, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS012", "固定资产", 1, 12, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS013", "无形资产", 1, 13, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS014", "非流动资产合计", 1, 14, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS015", "资产总计", 1, 15, 0, 0));
            templates.add(createTemplateRow(tenantId, "BS016", "流动负债", 1, 16, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS017", "短期借款", 1, 17, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS018", "应付票据", 1, 18, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS019", "应付账款", 1, 19, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS020", "预收款项", 1, 20, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS021", "应付职工薪酬", 1, 21, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS022", "应交税费", 1, 22, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS023", "流动负债合计", 1, 23, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS024", "非流动负债", 1, 24, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS025", "长期借款", 1, 25, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS026", "非流动负债合计", 1, 26, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS027", "负债合计", 1, 27, 0, 0));
            templates.add(createTemplateRow(tenantId, "BS028", "所有者权益", 1, 28, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS029", "实收资本", 1, 29, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS030", "资本公积", 1, 30, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS031", "盈余公积", 1, 31, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS032", "未分配利润", 1, 32, 1, 2));
            templates.add(createTemplateRow(tenantId, "BS033", "所有者权益合计", 1, 33, 0, 1));
            templates.add(createTemplateRow(tenantId, "BS034", "负债和所有者权益总计", 1, 34, 0, 0));
        } else if (reportType == ReportType.INCOME_STATEMENT.getCode()) {
            templates.add(createTemplateRow(tenantId, "IS001", "营业收入", 2, 1, 1, 0));
            templates.add(createTemplateRow(tenantId, "IS002", "营业成本", 2, 2, 1, 0));
            templates.add(createTemplateRow(tenantId, "IS003", "营业利润", 2, 3, 0, 0));
            templates.add(createTemplateRow(tenantId, "IS004", "营业税金及附加", 2, 4, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS005", "销售费用", 2, 5, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS006", "管理费用", 2, 6, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS007", "财务费用", 2, 7, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS008", "资产减值损失", 2, 8, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS009", "营业利润", 2, 9, 0, 0));
            templates.add(createTemplateRow(tenantId, "IS010", "营业外收入", 2, 10, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS011", "营业外支出", 2, 11, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS012", "利润总额", 2, 12, 0, 0));
            templates.add(createTemplateRow(tenantId, "IS013", "所得税费用", 2, 13, 1, 1));
            templates.add(createTemplateRow(tenantId, "IS014", "净利润", 2, 14, 0, 0));
        }
        return this.saveBatch(templates);
    }
    
    private ReportTemplate createTemplateRow(Long tenantId, String rowCode, String rowName, Integer reportType, Integer rowNo, Integer rowType, Integer level) {
        ReportTemplate template = new ReportTemplate();
        template.setTenantId(tenantId);
        template.setTemplateCode(rowCode);
        template.setTemplateName(rowName);
        template.setReportType(reportType);
        template.setRowNo(rowNo);
        template.setRowCode(rowCode);
        template.setRowName(rowName);
        template.setRowType(rowType);
        template.setLevel(level);
        template.setDisplayOrder(rowNo);
        template.setEnabled(1);
        return template;
    }
}