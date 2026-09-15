package cn.aiedge.dms.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DMS 配置实体
 */
@Data
@TableName("dms_config")
public class DmsConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String configKey;

    private String configValue;

    private String configDesc;

    private String scope;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    @Version
    private Integer version;

    // ── 以下为返回给前端的展示态字段（非表列）──

    /** 是否敏感配置（Key/密钥/令牌/密码）：敏感键的 configValue 返回**掩码**，明文不出服务端 */
    @TableField(exist = false)
    private Boolean secret;

    /** 是否已配置（敏感键判断「留空=不修改」与「清除」按钮显隐用） */
    @TableField(exist = false)
    private Boolean configured;
}
