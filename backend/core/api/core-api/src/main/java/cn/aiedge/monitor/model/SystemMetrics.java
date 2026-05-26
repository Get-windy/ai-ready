package cn.aiedge.monitor.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统指标
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SystemMetrics {

    /**
     * 采集时间
     */
    private LocalDateTime collectTime;

    // ==================== CPU指标 ====================

    /**
     * CPU使用率（%）
     */
    private Double cpuUsage;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 系统CPU使用率（%）
     */
    private Double systemCpuUsage;

    /**
     * 进程CPU使用率（%）
     */
    private Double processCpuUsage;

    // ==================== 内存指标 ====================

    /**
     * 总内存（MB）
     */
    private Long totalMemory;

    /**
     * 已用内存（MB）
     */
    private Long usedMemory;

    /**
     * 可用内存（MB）
     */
    private Long freeMemory;

    /**
     * 内存使用率（%）
     */
    private Double memoryUsage;

    /**
     * 堆内存使用（MB）
     */
    private Long heapUsed;

    /**
     * 堆内存最大（MB）
     */
    private Long heapMax;

    // ==================== 磁盘指标 ====================

    /**
     * 磁盘总空间（GB）
     */
    private Long totalDisk;

    /**
     * 磁盘已用空间（GB）
     */
    private Long usedDisk;

    /**
     * 磁盘可用空间（GB）
     */
    private Long freeDisk;

    /**
     * 磁盘使用率（%）
     */
    private Double diskUsage;

    // ==================== 网络指标 ====================

    /**
     * 网络入流量（KB/s）
     */
    private Long networkInRate;

    /**
     * 网络出流量（KB/s）
     */
    private Long networkOutRate;

    // ==================== JVM指标 ====================

    /**
     * JVM启动时间
     */
    private LocalDateTime jvmStartTime;

    /**
     * JVM运行时间（毫秒）
     */
    private Long jvmUptime;

    /**
     * 线程数
     */
    private Integer threadCount;

    /**
     * 峰值线程数
     */
    private Integer peakThreadCount;

    /**
     * 守护线程数
     */
    private Integer daemonThreadCount;

    /**
     * GC次数
     */
    private Long gcCount;

    /**
     * GC总耗时（毫秒）
     */
    private Long gcTime;

    // ==================== 系统负载 ====================

    /**
     * 系统负载（1分钟）
     */
    private Double systemLoad1;

    /**
     * 系统负载（5分钟）
     */
    private Double systemLoad5;

    /**
     * 系统负载（15分钟）
     */
    private Double systemLoad15;

    /**
     * 系统状态
     */
    private String systemStatus;
}
