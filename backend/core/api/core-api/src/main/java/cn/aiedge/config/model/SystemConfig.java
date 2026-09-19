package cn.aiedge.config.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置项（系统参数页的持久化载体）
 *
 * <p><b>2026-09-18 改造</b>：本类原先只是一个**普通模型**（无 {@code @TableName}、无 Mapper），
 * 实现层用 JVM 内存静态 Map「假装」读写 → 配置存不下、DB 的 {@code sys_config} 8 行永远读不到
 * （见《设置模块/系统参数开发文档.md》§5.3 / §7.2 / §12 P0）。本次把它接为 {@code sys_config}
 * 的真实实体：</p>
 *
 * <ul>
 *   <li>列名与属性名的错位用 {@code @TableField} 显式绑定（{@code param_key→configKey}、
 *       {@code param_name→configName}、{@code param_value→configValue}、{@code builtin→systemConfig}、
 *       {@code remark→description}）—— 这就是文档 §7.2 登记的「两套键名/列名对不上」的落点。</li>
 *   <li>新增列（{@code nav_group / parent_key / help_text / tip_text / locked}）见迁移
 *       {@code V11.393.0__Sys_Config_Real_Persistence_For_Sys_Params.sql} 的逐列说明。</li>
 *   <li>{@code defaultValue} 在 {@code sys_config} **没有对应列**（文档 §8.4 承诺不新增该列），
 *       故标 {@code exist = false}：字段保留是为了不改动已有接口契约，
 *       但**读写都不会落库**（文档 §12-⑳「无重置为默认值功能」的缺口如实保留）。</li>
 *   <li>{@code enabled / systemConfig / locked} 对应 PG 的 {@code boolean} 列，用 {@code Boolean}
 *       包装类型（列可空，用基本类型在命中 NULL 行时会类型不匹配）。</li>
 * </ul>
 */
@Schema(description = "系统配置项")
@TableName("sys_config")
public class SystemConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置键")
    @TableField("param_key")
    private String configKey;

    @Schema(description = "配置值")
    @TableField("param_value")
    private String configValue;

    @Schema(description = "配置类型: system/security/business/notification/integration")
    private String configType;

    @Schema(description = "配置分组")
    private String configGroup;

    @Schema(description = "配置名称")
    @TableField("param_name")
    private String configName;

    @Schema(description = "配置描述（落库列名 remark）")
    @TableField("remark")
    private String description;

    @Schema(description = "值类型: string/number/boolean/enum/list")
    private String valueType;

    /** ⚠️ `sys_config` 无 default_value 列，本字段不参与落库（见类注释） */
    @Schema(description = "默认值（当前不落库）")
    @TableField(exist = false)
    private String defaultValue;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "是否系统内置配置（落库列名 builtin）")
    @TableField("builtin")
    private Boolean systemConfig;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    // ── V11.393.0 新增列：左标签视图 / 父项 / 帮助气泡 / 温馨提示 / 不可逆锁定 ──

    @Schema(description = "左列纵向视图编码（industry/flow/bill/stock/finance/data_perm/notify/other）")
    @TableField("nav_group")
    private String navGroup;

    @Schema(description = "父配置键（可展开的父开关的子项）；顶层项为 null")
    @TableField("parent_key")
    private String parentKey;

    @Schema(description = "`?` 帮助气泡文案")
    @TableField("help_text")
    private String helpText;

    @Schema(description = "灰色「温馨提示」文案")
    @TableField("tip_text")
    private String tipText;

    @Schema(description = "不可逆配置锁定（锁定后页面禁止修改）")
    private Boolean locked;

    /**
     * 锁定原因（**不落库**，读取时按「商品引用」实时算出，供前端悬浮说明「为什么不能改」）
     *
     * <p>ql361 的语义是「此配置被商品启用后不能更改，请慎重选择」；本系统对应的真实引用链路
     * 见 {@code SystemConfigServiceImpl#applyReferenceLocks}。</p>
     */
    @Schema(description = "锁定原因（读取时实时计算，不落库）")
    @TableField(exist = false)
    private String lockedReason;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "租户ID（0 = 全局默认行）")
    private Long tenantId;

    @Schema(description = "逻辑删除标记（0 正常 / 1 已删）")
    @TableLogic
    private Integer deleted;

    /** 说明：`sys_config` 有 create_by / update_by 两列，但本页不维护操作人（存量 8 行亦为空），
     *  故不建属性 —— 不声明字段即不会被 MyBatis-Plus 写入，避免误覆盖。 */

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getConfigType() { return configType; }
    public void setConfigType(String configType) { this.configType = configType; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getSystemConfig() { return systemConfig; }
    public void setSystemConfig(Boolean systemConfig) { this.systemConfig = systemConfig; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getNavGroup() { return navGroup; }
    public void setNavGroup(String navGroup) { this.navGroup = navGroup; }
    public String getParentKey() { return parentKey; }
    public void setParentKey(String parentKey) { this.parentKey = parentKey; }
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    public String getTipText() { return tipText; }
    public void setTipText(String tipText) { this.tipText = tipText; }
    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }
    public String getLockedReason() { return lockedReason; }
    public void setLockedReason(String lockedReason) { this.lockedReason = lockedReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
