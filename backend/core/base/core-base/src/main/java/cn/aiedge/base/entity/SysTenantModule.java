package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 租户模块调用权配置实体
 * 记录租户购买的模块及其有效期，控制租户可使用的功能范围
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_tenant_module")
public class SysTenantModule {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 模块编码（如 order:sale, warehouse, finance）
     */
    private String moduleCode;

    /**
     * 模块名称（冗余，方便展示）
     */
    private String moduleName;

    /**
     * 购买类型：permanent-永久 auto_renew-自动续费 manual-手动延期
     */
    private String purchaseType;

    /**
     * 到期时间（permanent 类型可为 null）
     */
    private LocalDateTime expireTime;

    /**
     * 状态：0-正常 1-停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
