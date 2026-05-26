package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Data
@TableName("fin_data_source")
public class DataSource {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String dataSourceCode; // 数据源编码
    private String dataSourceName; // 数据源名称
    private String dataSourceType; // 数据源类型 (MYSQL, POSTGRESQL, ORACLE, SQLSERVER, EXCEL, API)
    private String connectionUrl; // 连接URL
    private String username; // 用户名
    private String password; // 密码（加密存储）
    private String driverClass; // 驱动类
    private String databaseName; // 数据库名
    
    private String parameters; // 连接参数 (JSON格式)
    private String sslConfig; // SSL配置 (JSON格式)
    
    private Boolean isEncrypted; // 是否加密
    private Boolean isActive; // 是否激活
    private Boolean isShared; // 是否共享
    
    private Integer maxConnections; // 最大连接数
    private Integer minConnections; // 最小连接数
    private Integer timeout; // 超时时间
    
    private String testQuery; // 测试查询语句
    private String description; // 描述
    
    private String createdBy; // 创建人
    private String updatedBy; // 更新人
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
