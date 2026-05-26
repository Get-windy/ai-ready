package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据源查询请求
 */
@Data
@Schema(description = "数据源查询请求")
public class DataSourceQueryRequest {

    @Schema(description = "数据源类型")
    private String dataSourceType; // MYSQL, POSTGRESQL, ORACLE, SQLSERVER, EXCEL, API

    @Schema(description = "数据源名称")
    private String dataSourceName;

    @Schema(description = "数据库名")
    private String databaseName;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "是否共享")
    private Boolean isShared;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
