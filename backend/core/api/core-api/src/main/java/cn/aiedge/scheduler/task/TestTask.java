package cn.aiedge.scheduler.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 测试任务类
 * 用于演示定时任务的执行
 */
@Slf4j
@Component
public class TestTask {

    /**
     * 执行任务
     * @param params 执行参数
     */
    public void execute(String params) {
        log.info("执行测试任务, 参数: {}", params);
        // 模拟任务执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("测试任务执行完成");
    }

    /**
     * 数据同步任务
     * @param params 同步参数
     */
    public void syncData(String params) {
        log.info("执行数据同步任务, 参数: {}", params);
        // 模拟数据同步
        log.info("数据同步完成");
    }

    /**
     * 报表生成任务
     * @param params 报表参数
     */
    public void generateReport(String params) {
        log.info("执行报表生成任务, 参数: {}", params);
        // 模拟报表生成
        log.info("报表生成完成");
    }
}
