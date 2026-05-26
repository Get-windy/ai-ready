package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据源VO
 */
@Data
@Schema(description = "数据源详情")
public class DataSourceVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "数据源编码")
    private String dataSourceCode;

    @Schema(description = "数据源名称")
    private String dataSourceName;

    @Schema(description = "数据源类型")
    private String dataSourceType; // MYSQL, POSTGRESQL, ORACLE, SQLSERVER, EXCEL, API

    @Schema(description = "连接URL")
    private String connectionUrl;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "驱动类")
    private String driverClass;

    @Schema(description = "数据库名")
    private String databaseName;

    @Schema(description = "连接参数")
    private String parameters; // JSON格式

    @Schema(description = "SSL配置")
    private String sslConfig; // JSON格式

    @Schema(description = "是否加密")
    private Boolean isEncrypted;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "是否共享")
    private Boolean isShared;

    @Schema(description = "最大连接数")
    private Integer maxConnections;

    @Schema(description = "最小连接数")
    private Integer minConnections;

    @Schema(description = "超时时间")
    private Integer timeout;

    @Schema(description = "测试查询语句")
    private String testQuery;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
