package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 数据源创建请求
 */
@Data
@Schema(description = "数据源创建请求")
public class DataSourceCreateRequest {

    @Schema(description = "数据源名称", required = true)
    @NotBlank(message = "数据源名称不能为空")
    private String dataSourceName;

    @Schema(description = "数据源类型", required = true)
    @NotBlank(message = "数据源类型不能为空")
    private String dataSourceType; // MYSQL, POSTGRESQL, ORACLE, SQLSERVER, EXCEL, API

    @Schema(description = "连接URL", required = true)
    @NotBlank(message = "连接URL不能为空")
    private String connectionUrl;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

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
}
