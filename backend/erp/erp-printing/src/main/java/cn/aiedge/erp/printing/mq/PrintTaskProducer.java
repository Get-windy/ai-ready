package cn.aiedge.erp.printing.mq;

import cn.aiedge.erp.printing.config.PrintingRabbitConfig;
import cn.aiedge.erp.printing.entity.v2.SysPrintTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 打印任务消息生产者
 * 向指定客户端推送打印任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintTaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 向指定客户端推送打印任务
     * 自动声明客户端队列（防止客户端首次上线前队列不存在）
     */
    public void sendTaskToClient(SysPrintTask task, Long clientId) {
        String routingKey = PrintingRabbitConfig.ROUTING_KEY_TASK_PREFIX + clientId;
        try {
            rabbitTemplate.convertAndSend(
                    PrintingRabbitConfig.EXCHANGE_PRINT,
                    routingKey,
                    task);
            log.info("MQ推送任务成功: taskCode={}, clientId={}, routingKey={}",
                    task.getTaskCode(), clientId, routingKey);
        } catch (Exception e) {
            log.error("MQ推送任务失败: taskCode={}, clientId={}", task.getTaskCode(), clientId, e);
        }
    }

    /**
     * 广播任务状态变更
     */
    public void sendStatusUpdate(Long taskId, String status, String clientId) {
        try {
            rabbitTemplate.convertAndSend(
                    PrintingRabbitConfig.EXCHANGE_PRINT,
                    PrintingRabbitConfig.ROUTING_KEY_STATUS,
                    new StatusMessage(taskId, status, clientId));
        } catch (Exception e) {
            log.error("MQ推送状态失败: taskId={}", taskId, e);
        }
    }

    public record StatusMessage(Long taskId, String status, String clientId) {}
}
