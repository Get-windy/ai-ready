package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ReportData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ReportDataService extends IService<ReportData> {
    
    List<ReportData> listByReportId(Long tenantId, Long reportId);
    
    boolean saveReportData(Long reportId, List<ReportData> data);
    
    boolean calculateRowValue(Long tenantId, Long reportId, String rowCode);
    
    boolean calculateAllRows(Long tenantId, Long reportId);
    
    Map<String, Object> getReportDataMap(Long tenantId, Long reportId);
}