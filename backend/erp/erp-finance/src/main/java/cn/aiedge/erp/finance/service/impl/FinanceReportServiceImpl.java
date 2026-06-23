package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.FinanceReportMapper;
import cn.aiedge.erp.finance.model.entity.FinanceReport;
import cn.aiedge.erp.finance.service.FinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 财务报表Service实现类
 */
@Service
public class FinanceReportServiceImpl implements FinanceReportService {

    @Autowired
    private FinanceReportMapper financeReportMapper;

    @Override
    public boolean generateReport(Integer reportType, String reportPeriod) {
        FinanceReport report = new FinanceReport();
        report.setReportNo("REPORT_" + LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        report.setReportType(reportType);
        report.setReportPeriod(reportPeriod);
        report.setStatus(1);
        report.setGeneratedBy("system");
        report.setGeneratedAt(LocalDateTime.now());

        financeReportMapper.insert(report);
        return true;
    }

    @Override
    public List<FinanceReport> listReports(Integer reportType, String reportPeriod, Integer status) {
        if (reportType != null) {
            return financeReportMapper.findByReportType(reportType);
        } else if (reportPeriod != null) {
            return financeReportMapper.findByReportPeriod(reportPeriod);
        } else if (status != null) {
            return financeReportMapper.findByStatus(status);
        }
        return financeReportMapper.selectList(null);
    }

    @Override
    public FinanceReport getReportDetail(String reportNo) {
        return financeReportMapper.findByReportNo(reportNo);
    }

    @Override
    public boolean approveReport(String reportNo, String approvedBy) {
        FinanceReport report = financeReportMapper.findByReportNo(reportNo);
        if (report == null) {
            return false;
        }
        report.setStatus(2);
        report.setApprovedBy(approvedBy);
        report.setApprovedAt(LocalDateTime.now());
        financeReportMapper.updateById(report);
        return true;
    }

    @Override
    public boolean deleteDraftReport(String reportNo) {
        FinanceReport report = financeReportMapper.findByReportNo(reportNo);
        if (report == null || report.getStatus() != 0) {
            return false;
        }
        report.setDeletedFlag(1);
        financeReportMapper.updateById(report);
        return true;
    }
}
