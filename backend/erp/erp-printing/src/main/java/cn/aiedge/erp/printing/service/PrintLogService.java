package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PrintLogService {

    Page<PrintLogDTO> queryLogs(PrintLogQueryRequest request);

    PrintStatisticsDTO getStatistics(String startDate, String endDate, String groupBy);

    byte[] exportLogs(String startDate, String endDate, String format);
}