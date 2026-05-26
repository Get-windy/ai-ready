package cn.aiedge.base.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 示例任务处理器
 * 演示如何在AI-Ready项目中使用XXL-Job进行任务调度
 */
@Component
public class SampleJobHandler {

    private static final Logger logger = LoggerFactory.getLogger(SampleJobHandler.class);

    /**
     * 简单示例任务
     * 模拟一些定时任务处理逻辑
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-Job示例任务开始执行");

        // 模拟业务处理
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("处理步骤: " + (i + 1));
            
            // 模拟耗时操作
            Thread.sleep(1000);
        }

        XxlJobHelper.log("XXL-Job示例任务执行完成");
    }

    /**
     * 数据清理任务示例
     */
    @XxlJob("dataCleanJobHandler")
    public void dataCleanJobHandler() throws Exception {
        XxlJobHelper.log("开始执行数据清理任务");

        // 这里可以放置实际的数据清理逻辑
        // 例如：清理过期的日志、临时文件、缓存等
        try {
            // 模拟数据清理操作
            int cleanedRecords = 100; // 实际清理的记录数
            XxlJobHelper.log("已清理 " + cleanedRecords + " 条过期数据");
            
            // 可以根据清理结果设置返回值
            XxlJobHelper.handleSuccess("成功清理 " + cleanedRecords + " 条记录");
        } catch (Exception e) {
            logger.error("数据清理任务执行失败", e);
            XxlJobHelper.handleFail("数据清理失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 系统监控任务示例
     */
    @XxlJob("monitorJobHandler")
    public void monitorJobHandler() throws Exception {
        XxlJobHelper.log("开始执行系统监控任务");

        try {
            // 获取系统信息
            long startTime = System.currentTimeMillis();
            
            // 模拟监控检查
            XxlJobHelper.log("检查系统状态...");
            Thread.sleep(500); // 模拟检查耗时
            
            XxlJobHelper.log("检查数据库连接...");
            Thread.sleep(300); // 模拟检查耗时
            
            XxlJobHelper.log("检查磁盘空间...");
            Thread.sleep(200); // 模拟检查耗时
            
            long endTime = System.currentTimeMillis();
            
            XxlJobHelper.log("系统监控任务完成，耗时: " + (endTime - startTime) + "ms");
            XxlJobHelper.handleSuccess("监控完成，耗时: " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            logger.error("系统监控任务执行失败", e);
            XxlJobHelper.handleFail("监控失败: " + e.getMessage());
            throw e;
        }
    }
}