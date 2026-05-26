package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.model.entity.FinanceReport;
import cn.aiedge.erp.finance.repository.FinanceReportRepository;
import cn.aiedge.erp.finance.service.FinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * 财务报表Service实现类
 */
@Service
public class FinanceReportServiceImpl implements FinanceReportService {
    
    @Autowired
    private FinanceReportRepository financeReportRepository;
    
    @Override
    public boolean generateReport(Integer reportType, String reportPeriod) {
        // 生成财务报表的逻辑
        FinanceReport report = new FinanceReport();
        report.setReportNo("REPORT_" + LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        report.setReportType(reportType);
        report.setReportPeriod(reportPeriod);
        report.setStatus(1);
        report.setGeneratedBy("system");
        report.setGeneratedAt(LocalDateTime.now());
        
        financeReportRepository.save(report);
        return true;
    }
    
    @Override
    public List<FinanceReport> listReports(Integer reportType, String reportPeriod, Integer status) {
        if (reportType != null) {
            return financeReportRepository.findByReportType(reportType);
        } else if (reportPeriod != null) {
            return financeReportRepository.findByReportPeriod(reportPeriod);
        } else if (status != null) {
            return financeReportRepository.findByStatus(status);
        }
        return financeReportRepository.findAll();
    }
    
    @Override
    public FinanceReport getReportDetail(String reportNo) {
        return financeReportRepository.findByReportNo(reportNo);
    }
    
    @Override
    public boolean approveReport(String reportNo, String approvedBy) {
        FinanceReport report = financeReportRepository.findByReportNo(reportNo);
        if (report == null) {
            return false;
        }
        report.setStatus(2);
        report.setApprovedBy(approvedBy);
        report.setApprovedAt(LocalDateTime.now());
        financeReportRepository.save(report);
        return true;
    }
    
    @Override
    public boolean deleteDraftReport(String reportNo) {
        FinanceReport report = financeReportRepository.findByReportNo(reportNo);
        if (report == null || report.getStatus() != 0) {
            return false;
        }
        financeReportRepository.delete(report);
        return true;
    }
}
