package com.qizhilian.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI-Ready 监控告警模块后端服务
 * 启动类
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-29
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MonitoringApplication {

    /**
     * 应用主入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MonitoringApplication.class, args);
    }

    /**
     * 应用启动完成回调
     * 
     * @return 欢迎信息
     */
    // @EventListener(ApplicationReadyEvent.class)
    // public void onApplicationReady() {
    //     System.out.println("\n" +
    //             "╔══════════════════════════════════════════════════════════╗\n" +
    //             "║                AI-Ready 监控告警模块启动成功                ║\n" +
    //             "║                    版本: 1.0.0                          ║\n" +
    //             "║                    端口: 8081                           ║\n" +
    //             "║               API路径: /api/monitoring                  ║\n" +
    //             "╚══════════════════════════════════════════════════════════╝\n");
    // }
}