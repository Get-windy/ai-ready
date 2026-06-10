package cn.aiedge.erp.printing.service.impl;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.PrintLog;
import cn.aiedge.erp.printing.entity.PrintTask;
import cn.aiedge.erp.printing.entity.PrintTemplate;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.enums.TaskStatus;
import cn.aiedge.erp.printing.mapper.PrintLogMapper;
import cn.aiedge.erp.printing.mapper.PrintTaskMapper;
import cn.aiedge.erp.printing.mapper.PrintTemplateMapper;
import cn.aiedge.erp.printing.mapper.PrinterMapper;
import cn.aiedge.erp.printing.service.PrintTaskService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrintTaskServiceImpl implements PrintTaskService {

    private final PrintTaskMapper taskMapper;
    private final PrintTemplateMapper templateMapper;
    private final PrinterMapper printerMapper;
    private final PrintLogMapper logMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SecurityContext securityContext;

    @Override
    @Transactional
    public PrintTask createTask(PrintTaskCreateRequest request, String clientIp) {
        PrintTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw BusinessException.notFound("模板不存在");
        }
        Printer printer = printerMapper.selectById(request.getPrinterId());
        if (printer == null) {
            throw BusinessException.notFound("打印机不存在");
        }
        if (!printer.getIsOnline()) {
            throw BusinessException.badRequest("打印机离线: " + printer.getPrinterName());
        }

        PrintTask task = new PrintTask();
        task.setTaskCode("TASK" + IdUtil.fastSimpleUUID().substring(0, 8));
        task.setTemplateId(request.getTemplateId());
        task.setPrinterId(request.getPrinterId());
        task.setDocumentId(request.getDocumentId());
        task.setDocumentType(request.getDocumentType());
        task.setDocumentNo(request.getDocumentNo());
        task.setPrintData(request.getPrintData());
        task.setCopies(request.getCopies() != null ? request.getCopies() : 1);
        task.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setSubmitTime(LocalDateTime.now());
        task.setOperatorName(request.getOperatorName());
        task.setClientIp(clientIp);
        taskMapper.insert(task);

        rabbitTemplate.convertAndSend("print.queue", task.getId());

        return task;
    }

    @Override
    @Transactional
    public BatchPrintResult batchPrint(BatchPrintRequest request, String clientIp) {
        BatchPrintResult result = new BatchPrintResult();
        result.setTotalCount(request.getDocumentIds().size());
        List<String> taskIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();

        for (String documentId : request.getDocumentIds()) {
            try {
                PrintTaskCreateRequest taskRequest = new PrintTaskCreateRequest();
                taskRequest.setTemplateId(request.getTemplateId());
                taskRequest.setPrinterId(request.getPrinterId());
                taskRequest.setDocumentId(documentId);
                taskRequest.setDocumentType(request.getDocumentType());
                taskRequest.setCopies(request.getCopies());
                taskRequest.setOperatorName(request.getOperatorName());
                PrintTask task = createTask(taskRequest, clientIp);
                taskIds.add(task.getId().toString());
            } catch (Exception e) {
                failedIds.add(documentId);
            }
        }

        result.setSuccessCount(taskIds.size());
        result.setFailedCount(failedIds.size());
        result.setTaskIds(taskIds);
        result.setFailedDocumentIds(failedIds);
        return result;
    }

    @Override
    public PrintTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<PrintTask> listTasks(Integer page, Integer size, String status, String documentType) {
        Page<PrintTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PrintTask::getStatus, status);
        }
        if (documentType != null && !documentType.isEmpty()) {
            wrapper.eq(PrintTask::getDocumentType, documentType);
        }
        wrapper.orderByDesc(PrintTask::getCreateTime);
        return taskMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<PrintTask> getPrintQueue() {
        Long tenantId = securityContext.getCurrentTenantId();
        return taskMapper.selectPendingTasks(100, tenantId);
    }

    @Override
    @Transactional
    public void cancelTask(Long id) {
        PrintTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(task.getTenantId())) {
            throw BusinessException.forbidden("无权操作其他租户的任务");
        }
        if (!task.getStatus().equals(TaskStatus.PENDING.getCode()) &&
            !task.getStatus().equals(TaskStatus.QUEUED.getCode())) {
            throw BusinessException.badRequest("任务状态不允许取消: " + task.getStatus());
        }
        task.setStatus(TaskStatus.CANCELLED.getCode());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional
    public void retryTask(Long id) {
        PrintTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(task.getTenantId())) {
            throw BusinessException.forbidden("无权操作其他租户的任务");
        }
        if (!task.getStatus().equals(TaskStatus.FAILED.getCode())) {
            throw BusinessException.badRequest("只有失败的任务可以重试");
        }
        if (task.getRetryCount() >= task.getMaxRetry()) {
            throw BusinessException.badRequest("已达到最大重试次数");
        }
        task.setStatus(TaskStatus.RETRYING.getCode());
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        rabbitTemplate.convertAndSend("print.queue", task.getId());
    }

    @Override
    @Transactional
    public void updateTaskStatus(Long id, String status, String errorMessage) {
        PrintTask task = taskMapper.selectById(id);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        if (status.equals(TaskStatus.PRINTING.getCode())) {
            task.setStartTime(LocalDateTime.now());
        } else if (status.equals(TaskStatus.COMPLETED.getCode()) || 
                   status.equals(TaskStatus.FAILED.getCode())) {
            task.setCompleteTime(LocalDateTime.now());
            if (task.getStartTime() != null) {
                task.setPrintDuration(
                    task.getCompleteTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() - 
                    task.getStartTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                );
            }
            createPrintLog(task);
        }
        taskMapper.updateById(task);
    }

    private void createPrintLog(PrintTask task) {
        PrintTemplate template = templateMapper.selectById(task.getTemplateId());
        Printer printer = printerMapper.selectById(task.getPrinterId());

        PrintLog log = new PrintLog();
        log.setTaskId(task.getId());
        log.setTaskCode(task.getTaskCode());
        log.setTemplateId(task.getTemplateId());
        log.setTemplateName(template != null ? template.getTemplateName() : null);
        log.setPrinterId(task.getPrinterId());
        log.setPrinterName(printer != null ? printer.getPrinterName() : null);
        log.setDocumentId(task.getDocumentId());
        log.setDocumentType(task.getDocumentType());
        log.setDocumentNo(task.getDocumentNo());
        log.setCopies(task.getCopies());
        log.setStatus(task.getStatus());
        log.setSuccess(task.getStatus().equals(TaskStatus.COMPLETED.getCode()));
        log.setErrorMessage(task.getErrorMessage());
        log.setPrintDuration(task.getPrintDuration());
        log.setPrintTime(LocalDateTime.now());
        log.setOperatorName(task.getOperatorName());
        logMapper.insert(log);
    }

    @Override
    public Page<PrintTask> getHistory(Integer page, Integer size, String startDate, String endDate, String documentType) {
        Page<PrintTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PrintTask::getStatus, TaskStatus.COMPLETED.getCode(), TaskStatus.FAILED.getCode(), TaskStatus.CANCELLED.getCode());
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(PrintTask::getCompleteTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(PrintTask::getCompleteTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
        }
        if (documentType != null && !documentType.isEmpty()) {
            wrapper.eq(PrintTask::getDocumentType, documentType);
        }
        wrapper.orderByDesc(PrintTask::getCompleteTime);
        return taskMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public int getQueueLength() {
        Long tenantId = securityContext.getCurrentTenantId();
        return taskMapper.countByStatus(TaskStatus.PENDING.getCode(), tenantId) +
               taskMapper.countByStatus(TaskStatus.QUEUED.getCode(), tenantId);
    }

    @Override
    @Transactional
    public void processQueue() {
        Long tenantId = securityContext.getCurrentTenantId();
        List<PrintTask> pendingTasks = taskMapper.selectPendingTasks(10, tenantId);
        for (PrintTask task : pendingTasks) {
            task.setStatus(TaskStatus.QUEUED.getCode());
            taskMapper.updateById(task);
        }
    }
}