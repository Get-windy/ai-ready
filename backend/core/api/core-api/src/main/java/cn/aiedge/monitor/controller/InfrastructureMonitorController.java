package cn.aiedge.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 基础设施监控控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/infrastructure")
@RequiredArgsConstructor
@Tag(name = "基础设施监控", description = "服务器、网络、存储等基础设施监控接口")
public class InfrastructureMonitorController {

    /**
     * 获取服务器基本信息
     */
    @GetMapping("/server/info")
    @Operation(summary = "获取服务器基本信息")
    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // 主机信息
            InetAddress localhost = InetAddress.getLocalHost();
            info.put("hostname", localhost.getHostName());
            info.put("ipAddress", localhost.getHostAddress());
            
            // 操作系统信息
            info.put("osName", System.getProperty("os.name"));
            info.put("osVersion", System.getProperty("os.version"));
            info.put("osArch", System.getProperty("os.arch"));
            
            // Java信息
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("javaVendor", System.getProperty("java.vendor"));
            
            // 运行时信息
            Runtime runtime = Runtime.getRuntime();
            info.put("availableProcessors", runtime.availableProcessors());
            
            // 系统时间
            info.put("serverTime", LocalDateTime.now().toString());
            info.put("timezone", TimeZone.getDefault().getID());
            
        } catch (Exception e) {
            log.error("Failed to get server info", e);
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * 获取CPU详细信息
     */
    @GetMapping("/cpu/detail")
    @Operation(summary = "获取CPU详细信息")
    public Map<String, Object> getCpuDetail() {
        Map<String, Object> detail = new HashMap<>();
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        detail.put("availableProcessors", osBean.getAvailableProcessors());
        detail.put("systemLoadAverage", osBean.getSystemLoadAverage());
        
        // 尝试获取更详细的CPU信息
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            detail.put("cpuLoad", sunOsBean.getCpuLoad());
            detail.put("systemCpuLoad", sunOsBean.getSystemCpuLoad());
            detail.put("processCpuLoad", sunOsBean.getProcessCpuLoad());
            
            // 物理内存信息
            Map<String, Object> physicalMemory = new HashMap<>();
            physicalMemory.put("totalPhysicalMemorySize", sunOsBean.getTotalPhysicalMemorySize());
            physicalMemory.put("freePhysicalMemorySize", sunOsBean.getFreePhysicalMemorySize());
            physicalMemory.put("committedVirtualMemorySize", sunOsBean.getCommittedVirtualMemorySize());
            detail.put("physicalMemory", physicalMemory);
        }
        
        detail.put("collectTime", LocalDateTime.now().toString());
        
        return detail;
    }

    /**
     * 获取磁盘信息
     */
    @GetMapping("/disk/info")
    @Operation(summary = "获取磁盘信息")
    public List<Map<String, Object>> getDiskInfo() {
        List<Map<String, Object>> disks = new ArrayList<>();
        
        try {
            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                Map<String, Object> disk = new HashMap<>();
                disk.put("path", root.toString());
                
                FileStore store = Files.getFileStore(root);
                disk.put("name", store.name());
                disk.put("type", store.type());
                
                long totalSpace = store.getTotalSpace();
                long usableSpace = store.getUsableSpace();
                long unallocatedSpace = store.getUnallocatedSpace();
                long usedSpace = totalSpace - usableSpace;
                
                disk.put("totalSpace", formatSize(totalSpace));
                disk.put("totalSpaceBytes", totalSpace);
                disk.put("usableSpace", formatSize(usableSpace));
                disk.put("usableSpaceBytes", usableSpace);
                disk.put("usedSpace", formatSize(usedSpace));
                disk.put("usedSpaceBytes", usedSpace);
                disk.put("usagePercent", String.format("%.2f%%", (double) usedSpace / totalSpace * 100));
                
                disks.add(disk);
            }
        } catch (Exception e) {
            log.error("Failed to get disk info", e);
        }
        
        // 备用方案：使用File类
        if (disks.isEmpty()) {
            File[] roots = File.listRoots();
            for (File root : roots) {
                Map<String, Object> disk = new HashMap<>();
                disk.put("path", root.getAbsolutePath());
                disk.put("totalSpace", formatSize(root.getTotalSpace()));
                disk.put("totalSpaceBytes", root.getTotalSpace());
                disk.put("freeSpace", formatSize(root.getFreeSpace()));
                disk.put("freeSpaceBytes", root.getFreeSpace());
                disk.put("usableSpace", formatSize(root.getUsableSpace()));
                disk.put("usableSpaceBytes", root.getUsableSpace());
                
                long used = root.getTotalSpace() - root.getFreeSpace();
                disk.put("usedSpace", formatSize(used));
                disk.put("usedSpaceBytes", used);
                disk.put("usagePercent", String.format("%.2f%%", (double) used / root.getTotalSpace() * 100));
                
                disks.add(disk);
            }
        }
        
        return disks;
    }

    /**
     * 获取网络接口信息
     */
    @GetMapping("/network/interfaces")
    @Operation(summary = "获取网络接口信息")
    public List<Map<String, Object>> getNetworkInterfaces() {
        List<Map<String, Object>> interfaces = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Map<String, Object> info = new HashMap<>();
                
                info.put("name", ni.getName());
                info.put("displayName", ni.getDisplayName());
                info.put("index", ni.getIndex());
                info.put("mtu", ni.getMTU());
                info.put("isUp", ni.isUp());
                info.put("isLoopback", ni.isLoopback());
                info.put("isVirtual", ni.isVirtual());
                info.put("supportsMulticast", ni.supportsMulticast());
                
                // IP地址
                List<String> addresses = new ArrayList<>();
                Enumeration<InetAddress> addrEnum = ni.getInetAddresses();
                while (addrEnum.hasMoreElements()) {
                    addresses.add(addrEnum.nextElement().getHostAddress());
                }
                info.put("addresses", addresses);
                
                // 子接口
                List<String> subInterfaces = new ArrayList<>();
                Enumeration<NetworkInterface> subEnum = ni.getSubInterfaces();
                while (subEnum.hasMoreElements()) {
                    subInterfaces.add(subEnum.nextElement().getName());
                }
                info.put("subInterfaces", subInterfaces);
                
                interfaces.add(info);
            }
        } catch (Exception e) {
            log.error("Failed to get network interfaces", e);
        }
        
        return interfaces;
    }

    /**
     * 获取网络统计信息
     */
    @GetMapping("/network/stats")
    @Operation(summary = "获取网络统计信息")
    public Map<String, Object> getNetworkStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 这里应该使用更专业的网络监控库
            // 简化实现，返回基础信息
            stats.put("collectTime", LocalDateTime.now().toString());
            stats.put("note", "Network statistics require platform-specific implementation or external libraries like OSHI");
            
            // 获取活跃连接数（简化）
            int activeConnections = 0;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    activeConnections++;
                }
            }
            stats.put("activeInterfaces", activeConnections);
            
        } catch (Exception e) {
            log.error("Failed to get network stats", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 获取进程信息
     */
    @GetMapping("/process/info")
    @Operation(summary = "获取进程信息")
    public Map<String, Object> getProcessInfo() {
        Map<String, Object> info = new HashMap<>();
        
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        info.put("processId", getProcessId(runtimeBean));
        info.put("processName", System.getProperty("sun.java.command", "Unknown"));
        info.put("startTime", new Date(runtimeBean.getStartTime()).toString());
        info.put("uptime", formatDuration(runtimeBean.getUptime()));
        info.put("uptimeMs", runtimeBean.getUptime());
        
        // JVM参数
        info.put("vmArguments", runtimeBean.getInputArguments());
        
        return info;
    }

    /**
     * 获取环境信息
     */
    @GetMapping("/environment")
    @Operation(summary = "获取环境信息")
    public Map<String, Object> getEnvironment() {
        Map<String, Object> env = new HashMap<>();
        
        // 系统环境变量
        env.put("environmentVariables", System.getenv());
        
        // 系统属性
        Map<String, String> systemProps = new HashMap<>();
        System.getProperties().forEach((k, v) -> {
            if (k.toString().startsWith("java.") || 
                k.toString().startsWith("os.") || 
                k.toString().startsWith("user.")) {
                systemProps.put(k.toString(), v.toString());
            }
        });
        env.put("systemProperties", systemProps);
        
        return env;
    }

    // ==================== 私有方法 ====================

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String getProcessId(RuntimeMXBean runtimeBean) {
        String name = runtimeBean.getName();
        int index = name.indexOf('@');
        return index > 0 ? name.substring(0, index) : name;
    }
}