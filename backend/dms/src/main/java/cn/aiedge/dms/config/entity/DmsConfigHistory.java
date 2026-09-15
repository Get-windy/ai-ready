package cn.aiedge.dms.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置变更审计（表 dms_config_history）
 *
 * 敏感键（Key/密钥/令牌/密码）的 `oldValue/newValue` 存**掩码**；明文另存 `*Cipher` 列仅供服务端回滚，
 * **任何接口都不返回**（避免审计表成为密钥泄露面）。
 */
@Data
@TableName("dms_config_history")
public class DmsConfigHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String configKey;

    /** 变更前值（敏感键为掩码） */
    private String oldValue;

    /** 变更后值（敏感键为掩码） */
    private String newValue;

    /** 变更前明文（仅敏感键落库，服务端回滚用，不对外返回） */
    @TableField(select = false)
    private String oldValueCipher;

    /** 变更后明文（同上） */
    @TableField(select = false)
    private String newValueCipher;

    /** 是否敏感键：0-否 1-是 */
    private Integer secret;

    /** CREATE / UPDATE / CLEAR / ROLLBACK */
    private String changeType;

    private Long operatorId;

    private String operatorName;

    private String clientIp;

    private LocalDateTime changeTime;

    private String remark;
}
