package cn.aiedge.datasource.support;

import cn.aiedge.datasource.mapper.DataSourceMapper;
import cn.aiedge.datasource.model.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 备份/恢复的目标连接解析器
 *
 * <p>解析口径（两条来源，**都显式回显给调用方**，不做静默替换）：
 * <ol>
 *   <li><b>传了 dataSourceId</b> → 读 {@code sys_data_source}。记录不存在、或 dbType 不是 PostgreSQL
 *       一律**明确失败** —— 绝不「自动换个库备份」，那会让用户以为备份的是自己选的那个库。</li>
 *   <li><b>没传 dataSourceId</b> → 用本应用自身的 {@code spring.datasource.*}（即本系统数据库），
 *       此时 {@code origin=spring.datasource}，页面/接口返回里会写明，用户不会误读。</li>
 * </ol>
 *
 * <p>为什么不支持 MySQL/Oracle/SQLServer：本模块的执行器是 {@code pg_dump}/{@code pg_restore}，
 * 只对 PostgreSQL 成立。对其它类型**诚实报错**比给一个「什么都没做但返回成功」的实现更有用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupConnectionResolver {

    /** 解析 jdbc:postgresql://host:port/db?xxx */
    private static final Pattern PG_URL =
            Pattern.compile("^jdbc:postgresql://([^:/?]+)(?::(\\d+))?/([^?;]+)", Pattern.CASE_INSENSITIVE);

    private final DataSourceMapper dataSourceMapper;

    @Value("${spring.datasource.url:}")
    private String appDatasourceUrl;

    @Value("${spring.datasource.username:}")
    private String appDatasourceUsername;

    @Value("${spring.datasource.password:}")
    private String appDatasourcePassword;

    /**
     * 解析目标连接
     *
     * @param dataSourceId 数据源ID，可为 null（null 表示备份/恢复本系统数据库）
     * @throws IllegalArgumentException 参数不合法或不受支持时抛出（消息可直接展示给用户）
     */
    public PgConnectionTarget resolve(Long dataSourceId) {
        if (dataSourceId == null) {
            return fromApplicationDatasource();
        }
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null || Integer.valueOf(1).equals(ds.getDeleted())) {
            throw new IllegalArgumentException(
                    "数据源 #" + dataSourceId + " 不存在（sys_data_source 无该记录）。"
                            + "备份/恢复只接受已登记的数据源，或留空表示操作本系统数据库。");
        }
        String dbType = ds.getDbType() == null ? "" : ds.getDbType().trim().toLowerCase();
        if (!"postgresql".equals(dbType) && !"postgres".equals(dbType)) {
            throw new IllegalArgumentException(
                    "数据源「" + ds.getName() + "」类型为 " + ds.getDbType() + "，"
                            + "本模块的执行器基于 pg_dump/pg_restore，仅支持 PostgreSQL 数据源。");
        }
        if (!StringUtils.hasText(ds.getHost()) || !StringUtils.hasText(ds.getDatabaseName())) {
            throw new IllegalArgumentException("数据源「" + ds.getName() + "」缺少主机或库名，无法备份");
        }
        int port = ds.getPort() == null ? 5432 : ds.getPort();
        String desc = ds.getHost() + ":" + port + "/" + ds.getDatabaseName() + "（数据源「" + ds.getName() + "」#" + ds.getId() + "）";
        return new PgConnectionTarget(
                ds.getHost().trim(),
                port,
                ds.getDatabaseName().trim(),
                ds.getUsername() == null ? "" : ds.getUsername(),
                ds.getPassword() == null ? "" : ds.getPassword(),
                "sys_data_source",
                desc);
    }

    /** 回退到本应用自身的数据库连接（未指定数据源时） */
    private PgConnectionTarget fromApplicationDatasource() {
        if (!StringUtils.hasText(appDatasourceUrl)) {
            throw new IllegalArgumentException("未配置 spring.datasource.url，且未指定数据源，无法确定备份目标");
        }
        Matcher m = PG_URL.matcher(appDatasourceUrl.trim());
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "未指定数据源，且本系统数据库不是 PostgreSQL（spring.datasource.url 无法解析为 pg 连接），"
                            + "请在「连接管理」登记要备份的 PostgreSQL 数据源后重试。");
        }
        String host = m.group(1);
        int port = m.group(2) != null ? Integer.parseInt(m.group(2)) : 5432;
        String database = m.group(3);
        String desc = host + ":" + port + "/" + database + "（本系统数据库，来自 spring.datasource）";
        return new PgConnectionTarget(host, port, database,
                appDatasourceUsername == null ? "" : appDatasourceUsername,
                appDatasourcePassword == null ? "" : appDatasourcePassword,
                "spring.datasource",
                desc);
    }
}
