package cn.aiedge.datasource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 数据管理（备份 / 同步 / 清理）运维动作配置
 *
 * <p>配置前缀 {@code app.data-maintenance}。全部字段都有安全默认值 ——
 * 未在 yml 中显式配置时也能以「最小权限」运行（清理白名单默认只放行日志/审计表）。
 *
 * <p>口径说明：
 * <ul>
 *   <li>本类**不**定义同步引擎地址 —— 同步引擎的地址只有一个事实来源
 *       {@code cn.aiedge.integration.SyncConfigServiceImpl} 的 {@code sync-engine.api-url}，
 *       本模块直接复用该服务，避免同一配置两处维护（见 MEMORY「配置落位总则」）。</li>
 *   <li>备份目录默认留空，运行时回退到 {@code storage.local.base-path} 的同级 {@code backups} 目录，
 *       保证「多实例同一份备份目录」（见 MEMORY「上传根目录随启动目录漂移」同类陷阱）。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.data-maintenance")
public class DataMaintenanceProperties {

    private Backup backup = new Backup();

    private Sync sync = new Sync();

    private Cleanup cleanup = new Cleanup();

    /** 备份（pg_dump / pg_restore） */
    @Data
    public static class Backup {

        /**
         * 备份文件落盘根目录（绝对路径）。留空时按 storage.local.base-path 的同级 backups 目录推导。
         * <p>⚠️ 该目录是**服务端唯一可信根**：所有来自 DB/前端的路径都必须 normalize 后仍位于其内，
         * 否则一律拒绝（防目录穿越）。
         */
        private String dir = "";

        /**
         * PostgreSQL 客户端工具目录（包含 pg_dump / pg_restore）。
         * 留空时依次尝试：系统 PATH → Windows 常见安装目录（C:/Program Files/PostgreSQL/&lt;ver&gt;/bin，取最高版本）。
         */
        private String pgBinDir = "";

        /** 单次 pg_dump 超时（秒）。超时即强杀进程并置 failed，绝不留下 running */
        private int dumpTimeoutSeconds = 600;

        /** 单次 pg_restore 超时（秒）。恢复为同步执行（需立即给出真实结论），故不宜过大 */
        private int restoreTimeoutSeconds = 600;

        /** 备份文件最小可用磁盘余量（MB）。低于该值直接拒绝创建，避免写坏文件 */
        private long minFreeSpaceMb = 512;

        /** 进程输出（stdout+stderr）最多保留的字符数，超出截断 —— 避免错误信息撑爆 error_message */
        private int maxOutputChars = 2000;
    }

    /** 同步（投递到 sync-engine） */
    @Data
    public static class Sync {

        /**
         * 在途判定窗口（秒）。{@code last_run_status=running} 且距 {@code last_run_time} 未超过该窗口时，
         * 视为「同一任务正在执行」，重复调用直接拒绝（幂等闸门，防双击/并发重复投递）。
         * 超过窗口则视为上次执行异常中断（进程被杀等），允许重新执行并复位在途标记。
         */
        private int inflightTimeoutSeconds = 600;
    }

    /** 清理（按保留天数删除历史数据） */
    @Data
    public static class Cleanup {

        /**
         * 目标表**白名单**（服务端唯一可信来源，绝不接受前端传来的表名直接拼 SQL）。
         * <p>默认只放行「纯追加的日志/审计表」—— 这些表按保留期删除历史行是既有运维语义；
         * 业务单据表、主数据表**默认不放行**，需要清理时由运维显式追加。
         * <p>白名单之外还会做③重校验：标识符正则 + information_schema 存在性 + 条件列为时间类型。
         */
        private List<String> allowedTables = new ArrayList<>(Arrays.asList(
                "sys_oper_log",
                "sys_login_log",
                "sys_audit_log",
                "sys_system_log",
                "sys_job_log",
                "scheduled_task_log",
                "api_access_log",
                "gateway_log",
                "dms_task_log",
                "sync_history"
        ));

        /** 每批删除行数（分批删除，避免单条大事务长时间持锁并撑大 WAL） */
        private int batchSize = 1000;

        /**
         * 单次执行最多删除行数（**上限保护**）。达到上限即停止并回报「已截断」，
         * 防止保留天数写错（如 0）时一次删空整表。
         */
        private long maxRowsPerRun = 5000;

        /**
         * 最短保留天数。{@code retentionDays < 该值} 一律拒绝（retentionDays=0 等价于「全表删除」）。
         */
        private int minRetentionDays = 1;

        /**
         * 是否要求条件列必须是**时间类型**列。
         * 默认 true —— 「保留 N 天」的语义只对时间列成立；关掉只应在特殊场景（如按 id 水位清理），
         * 且需要同步评估删除边界是否可确定。
         */
        private boolean requireTemporalColumn = true;
    }
}
