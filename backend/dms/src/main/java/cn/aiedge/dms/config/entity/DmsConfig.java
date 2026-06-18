package cn.aiedge.dms.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
}
