package cn.aiedge.erp.printing.mq;

import cn.aiedge.erp.printing.entity.v2.SysPrintTask;
import cn.aiedge.erp.printing.mapper.SysPrintTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 打印任务消息消费者
 * 监听客户端回执的状态更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintTaskConsumer {

    private final SysPrintTaskMapper taskMapper;

    @RabbitListener(queues = "${print.status.queue:print.status.queue}")
    @Transactional
    public void handleStatusUpdate(PrintTaskProducer.StatusMessage message) {
        if (message == null || message.taskId() == null) return;

        SysPrintTask task = taskMapper.selectById(message.taskId());
        if (task == null) {
            log.warn("MQ收到状态更新但任务不存在: taskId={}", message.taskId());
            return;
        }

        String newStatus = message.status();
        task.setStatus(newStatus);

        if ("COMPLETED".equals(newStatus)) {
            task.setCompleteTime(LocalDateTime.now());
        } else if ("FAILED".equals(newStatus)) {
            task.setErrorMessage("客户端上报失败");
        }

        taskMapper.updateById(task);
        log.info("MQ处理状态更新: taskId={}, status={}", message.taskId(), newStatus);
    }

    // 任务分发由客户端直接消费 per-client 队列 (print.task.{clientId})，无需服务端监听
}
