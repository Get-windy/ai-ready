package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.ReportData;
import cn.aiedge.finance.mapper.ReportDataMapper;
import cn.aiedge.finance.service.ReportDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportDataServiceImpl extends ServiceImpl<ReportDataMapper, ReportData> implements ReportDataService {
    
    @Override
    public List<ReportData> listByReportId(Long tenantId, Long reportId) {
        return baseMapper.listByReportId(tenantId, reportId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReportData(Long reportId, List<ReportData> data) {
        for (ReportData item : data) {
            item.setReportId(reportId);
            this.save(item);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calculateRowValue(Long tenantId, Long reportId, String rowCode) {
        ReportData data = baseMapper.getByReportIdAndRowCode(tenantId, reportId, rowCode);
        if (data == null) return false;
        return this.updateById(data);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calculateAllRows(Long tenantId, Long reportId) {
        List<ReportData> dataList = this.listByReportId(tenantId, reportId);
        for (ReportData data : dataList) {
            this.updateById(data);
        }
        return true;
    }
    
    @Override
    public Map<String, Object> getReportDataMap(Long tenantId, Long reportId) {
        List<ReportData> dataList = this.listByReportId(tenantId, reportId);
        Map<String, Object> result = new HashMap<>();
        for (ReportData data : dataList) {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("rowNo", data.getRowNo());
            rowMap.put("rowCode", data.getRowCode());
            rowMap.put("rowName", data.getRowName());
            rowMap.put("currentAmount", data.getCurrentAmount());
            rowMap.put("previousAmount", data.getPreviousAmount());
            rowMap.put("yearAmount", data.getYearAmount());
            rowMap.put("rowType", data.getRowType());
            rowMap.put("level", data.getLevel());
            result.put(data.getRowCode(), rowMap);
        }
        return result;
    }
}