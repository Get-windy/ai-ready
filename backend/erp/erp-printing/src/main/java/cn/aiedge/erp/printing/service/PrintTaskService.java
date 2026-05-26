package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.entity.PrintTask;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface PrintTaskService {

    PrintTask createTask(PrintTaskCreateRequest request, String clientIp);

    BatchPrintResult batchPrint(BatchPrintRequest request, String clientIp);

    PrintTask getTaskById(Long id);

    Page<PrintTask> listTasks(Integer page, Integer size, String status, String documentType);

    List<PrintTask> getPrintQueue();

    void cancelTask(Long id);

    void retryTask(Long id);

    void updateTaskStatus(Long id, String status, String errorMessage);

    Page<PrintTask> getHistory(Integer page, Integer size, String startDate, String endDate, String documentType);

    int getQueueLength();

    void processQueue();
}