package cn.aiedge.erp.printing.service;

import cn.aiedge.erp.printing.dto.v2.ChainTaskExecuteRequest;
import cn.aiedge.erp.printing.dto.v2.PrintTaskVO;

import java.util.List;

/**
 * 链路执行器 —— 将打印链路拆分为多个子任务并分发
 */
public interface ChainExecutorService {

    /**
     * 按链路执行打印
     * 将链路的每一步拆分为独立的 PrintTask，依次/并行推送到各客户端
     *
     * @param request  执行请求
     * @param tenantId 租户 ID
     * @param userId   操作人
     * @return 创建的任务列表
     */
    List<PrintTaskVO> executeChain(ChainTaskExecuteRequest request, Long tenantId, Long userId);

    /**
     * 执行链路中的单个步骤
     *
     * @param taskId 任务 ID
     */
    void executeStep(Long taskId);

    /**
     * 确认截图（手动确认模式）
     * 用户确认截图后，继续执行打印
     *
     * @param taskId 任务 ID
     */
    void confirmScreenshot(Long taskId);

    /**
     * 取消等待截图确认的任务
     *
     * @param taskId 任务 ID
     */
    void cancelWaitingTask(Long taskId);
}
