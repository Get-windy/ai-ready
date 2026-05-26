package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.PrintLog;
import cn.aiedge.erp.printing.mapper.PrintLogMapper;
import cn.aiedge.erp.printing.service.PrintLogService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrintLogServiceImpl implements PrintLogService {

    private final PrintLogMapper logMapper;

    @Override
    public Page<PrintLogDTO> queryLogs(PrintLogQueryRequest request) {
        Page<PrintLog> pageObj = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<PrintLog> wrapper = new LambdaQueryWrapper<>();
        if (request.getStartDate() != null) {
            wrapper.ge(PrintLog::getPrintTime, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(PrintLog::getPrintTime, request.getEndDate().atTime(LocalTime.MAX));
        }
        if (request.getPrinterId() != null) {
            wrapper.eq(PrintLog::getPrinterId, request.getPrinterId());
        }
        if (request.getTemplateId() != null) {
            wrapper.eq(PrintLog::getTemplateId, request.getTemplateId());
        }
        if (request.getDocumentType() != null && !request.getDocumentType().isEmpty()) {
            wrapper.eq(PrintLog::getDocumentType, request.getDocumentType());
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            wrapper.eq(PrintLog::getStatus, request.getStatus());
        }
        if (request.getSuccess() != null) {
            wrapper.eq(PrintLog::getSuccess, request.getSuccess());
        }
        if (request.getOperatorName() != null && !request.getOperatorName().isEmpty()) {
            wrapper.like(PrintLog::getOperatorName, request.getOperatorName());
        }
        wrapper.orderByDesc(PrintLog::getPrintTime);

        Page<PrintLog> logPage = logMapper.selectPage(pageObj, wrapper);
        Page<PrintLogDTO> dtoPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        dtoPage.setRecords(convertToDTO(logPage.getRecords()));
        return dtoPage;
    }

    private List<PrintLogDTO> convertToDTO(List<PrintLog> logs) {
        return logs.stream().map(log -> {
            PrintLogDTO dto = new PrintLogDTO();
            dto.setId(log.getId());
            dto.setTaskId(log.getTaskId());
            dto.setTaskCode(log.getTaskCode());
            dto.setTemplateId(log.getTemplateId());
            dto.setTemplateName(log.getTemplateName());
            dto.setPrinterId(log.getPrinterId());
            dto.setPrinterName(log.getPrinterName());
            dto.setDocumentId(log.getDocumentId());
            dto.setDocumentType(log.getDocumentType());
            dto.setDocumentNo(log.getDocumentNo());
            dto.setCopies(log.getCopies());
            dto.setStatus(log.getStatus());
            dto.setSuccess(log.getSuccess());
            dto.setErrorMessage(log.getErrorMessage());
            dto.setPrintDuration(log.getPrintDuration());
            dto.setPrintTime(log.getPrintTime());
            dto.setOperatorName(log.getOperatorName());
            dto.setOperatorId(log.getOperatorId());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public PrintStatisticsDTO getStatistics(String startDate, String endDate, String groupBy) {
        LocalDateTime startTime = startDate != null ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endTime = endDate != null ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : LocalDateTime.now();

        PrintStatisticsDTO dto = new PrintStatisticsDTO();

        Map<String, Object> stats = logMapper.selectStatistics(startTime, endTime);
        dto.setTotalPrints(((Number) stats.getOrDefault("total", 0L)).longValue());
        dto.setSuccessPrints(((Number) stats.getOrDefault("success", 0L)).longValue());
        dto.setFailedPrints(((Number) stats.getOrDefault("failed", 0L)).longValue());
        dto.setTotalDuration(((Number) stats.getOrDefault("total_duration", 0L)).longValue());
        dto.setAvgDuration(((Number) stats.getOrDefault("avg_duration", 0D)).doubleValue());

        List<Map<String, Object>> printerStats = logMapper.selectPrinterStats(startTime, endTime);
        dto.setPrinterStats(convertStatsList(printerStats, "printer_name", "count"));

        List<Map<String, Object>> templateStats = logMapper.selectTemplateStats(startTime, endTime);
        dto.setTemplateStats(convertStatsList(templateStats, "template_name", "count"));

        List<Map<String, Object>> docTypeStats = logMapper.selectDocumentTypeStats(startTime, endTime);
        dto.setDocumentTypeStats(convertStatsList(docTypeStats, "document_type", "count"));

        return dto;
    }

    private Map<String, Long> convertStatsList(List<Map<String, Object>> list, String keyField, String valueField) {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> item : list) {
            String key = (String) item.get(keyField);
            Long value = ((Number) item.get(valueField)).longValue();
            result.put(key, value);
        }
        return result;
    }

    @Override
    public byte[] exportLogs(String startDate, String endDate, String format) {
        LocalDateTime startTime = startDate != null ? LocalDate.parse(startDate).atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endTime = endDate != null ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : LocalDateTime.now();

        LambdaQueryWrapper<PrintLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PrintLog::getPrintTime, startTime)
               .le(PrintLog::getPrintTime, endTime)
               .orderByDesc(PrintLog::getPrintTime);
        List<PrintLog> logs = logMapper.selectList(wrapper);

        if ("excel".equalsIgnoreCase(format)) {
            return exportToExcel(logs);
        }
        return exportToExcel(logs);
    }

    private byte[] exportToExcel(List<PrintLog> logs) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("taskCode", "任务编号");
        writer.addHeaderAlias("templateName", "模板名称");
        writer.addHeaderAlias("printerName", "打印机");
        writer.addHeaderAlias("documentType", "文档类型");
        writer.addHeaderAlias("documentNo", "文档编号");
        writer.addHeaderAlias("copies", "份数");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("success", "是否成功");
        writer.addHeaderAlias("printDuration", "打印耗时(ms)");
        writer.addHeaderAlias("printTime", "打印时间");
        writer.addHeaderAlias("operatorName", "操作人");

        List<Map<String, Object>> rows = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (PrintLog log : logs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("taskCode", log.getTaskCode());
            row.put("templateName", log.getTemplateName());
            row.put("printerName", log.getPrinterName());
            row.put("documentType", log.getDocumentType());
            row.put("documentNo", log.getDocumentNo());
            row.put("copies", log.getCopies());
            row.put("status", log.getStatus());
            row.put("success", log.getSuccess() ? "成功" : "失败");
            row.put("printDuration", log.getPrintDuration());
            row.put("printTime", log.getPrintTime() != null ? log.getPrintTime().format(formatter) : "");
            row.put("operatorName", log.getOperatorName());
            rows.add(row);
        }
        writer.write(rows, true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.flush(out, true);
        writer.close();
        return out.toByteArray();
    }
}