package cn.aiedge.config.mapper;

import cn.aiedge.config.model.SystemConfig;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 系统参数（{@code sys_config}）Mapper
 *
 * <p><b>为什么全部手写 SQL 并逐条 {@code @InterceptorIgnore(tenantLine = "true")}？</b></p>
 *
 * <p>{@code sys_config} **有** {@code tenant_id} 列，所以它不在
 * {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 里，多租户插件会注入 {@code tenant_id = <会话租户>}。
 * 但本表的设计口径是「<b>{@code tenant_id = 0} 全局默认行 + 租户行</b>」：迁移
 * {@code V11.393.0} 里 seed 的配置项与存量 8 行**全部是 {@code tenant_id = 0}**。
 * 一旦让插件注入 {@code tenant_id = 1}，这些行就**永远读不到** —— 这正是文档 §7.2
 * 「DB 的 {@code sys_config} 8 行永远读不到」的 P0（同源问题另见
 * 《全局默认配置被租户插件过滤》《配送参数开发文档》§7）。</p>
 *
 * <p>因此：读/写路径**统一显式关闭多租户插件**，并在 SQL 里自己写
 * {@code tenant_id IN (0, #{tenantId})}（只读场景）或按行归属精确定位（写入场景）。
 * 调用方必须传入 {@code tenantId}（{@code SystemConfigController} 已把
 * {@code X-Tenant-Id} 透传下来，服务层在为空时回退会话租户）。</p>
 *
 * <p>列名与属性名错位（{@code param_key→configKey} 等）通过 SQL 别名显式对齐，
 * 不依赖 {@code map-underscore-to-camel-case}。</p>
 */
@Mapper
public interface SysConfigMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<SystemConfig> {

    /** 查询列（别名 = 实体属性名，兼容下划线转驼峰关闭的场景） */
    String COLS = "id, param_key AS \"configKey\", param_name AS \"configName\", param_value AS \"configValue\", "
            + "config_type AS \"configType\", config_group AS \"configGroup\", value_type AS \"valueType\", "
            + "builtin AS \"systemConfig\", remark AS \"description\", sort_order AS \"sortOrder\", "
            + "enabled AS \"enabled\", nav_group AS \"navGroup\", parent_key AS \"parentKey\", "
            + "help_text AS \"helpText\", tip_text AS \"tipText\", locked AS \"locked\", "
            + "tenant_id AS \"tenantId\", create_time AS \"createTime\", update_time AS \"updateTime\", "
            + "deleted AS \"deleted\"";

    /**
     * 按条件查询参数列表（全局默认行 tenant_id = 0 与指定租户行都可见）
     *
     * <p>字符串条件口径：**null = 不限，非 null 才参与过滤**。调用方需先把「空串」归一为 null
     * （`SystemConfigServiceImpl` 的 {@code normalizeQuery} 已统一处理），
     * 这样 SQL 里就不需要 `!= ''` 这类引号判断（避免在注解读的 XML script 里写实体转义）。</p>
     *
     * @param tenantId    当前租户；为 null 时不加租户条件（无会话租户的平台超管）
     * @param configType  配置类型（等值，null = 不限）
     * @param configGroup 配置分组（null = 不限）
     * @param navGroup    左标签视图编码（null = 不限，本页按视图取数用）
     * @param configKey   配置键（模糊）
     * @param configName  配置名称（模糊）
     * @param enabled     是否启用（null = 不限）
     * @param builtin     是否内置（null = 不限）
     * @param limit       分页大小（null = 不分页）
     * @param offset      分页偏移（从 0 开始）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
            + "SELECT " + COLS + " FROM sys_config WHERE deleted = 0 "
            + "<if test='tenantId != null'> AND tenant_id IN (0, #{tenantId}) </if>"
            + "<if test='configType != null'> AND config_type = #{configType} </if>"
            + "<if test='configGroup != null'> AND config_group = #{configGroup} </if>"
            + "<if test='navGroup != null'> AND nav_group = #{navGroup} </if>"
            + "<if test='configKey != null'> AND param_key LIKE CONCAT('%', #{configKey}, '%') </if>"
            + "<if test='configName != null'> AND param_name LIKE CONCAT('%', #{configName}, '%') </if>"
            + "<if test='enabled != null'> AND enabled = #{enabled} </if>"
            + "<if test='builtin != null'> AND builtin = #{builtin} </if>"
            + "ORDER BY sort_order ASC NULLS LAST, id ASC "
            + "<if test='limit != null'> LIMIT #{limit} OFFSET #{offset} </if>"
            + "</script>")
    List<SystemConfig> selectConfigs(@Param("tenantId") Long tenantId,
                                     @Param("configType") String configType,
                                     @Param("configGroup") String configGroup,
                                     @Param("navGroup") String navGroup,
                                     @Param("configKey") String configKey,
                                     @Param("configName") String configName,
                                     @Param("enabled") Boolean enabled,
                                     @Param("builtin") Boolean builtin,
                                     @Param("limit") Integer limit,
                                     @Param("offset") Integer offset);

    /** 与 {@link #selectConfigs} 同条件的总数（真分页的 total 口径） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
            + "SELECT COUNT(*) FROM sys_config WHERE deleted = 0 "
            + "<if test='tenantId != null'> AND tenant_id IN (0, #{tenantId}) </if>"
            + "<if test='configType != null'> AND config_type = #{configType} </if>"
            + "<if test='configGroup != null'> AND config_group = #{configGroup} </if>"
            + "<if test='navGroup != null'> AND nav_group = #{navGroup} </if>"
            + "<if test='configKey != null'> AND param_key LIKE CONCAT('%', #{configKey}, '%') </if>"
            + "<if test='configName != null'> AND param_name LIKE CONCAT('%', #{configName}, '%') </if>"
            + "<if test='enabled != null'> AND enabled = #{enabled} </if>"
            + "<if test='builtin != null'> AND builtin = #{builtin} </if>"
            + "</script>")
    long countConfigs(@Param("tenantId") Long tenantId,
                      @Param("configType") String configType,
                      @Param("configGroup") String configGroup,
                      @Param("navGroup") String navGroup,
                      @Param("configKey") String configKey,
                      @Param("configName") String configName,
                      @Param("enabled") Boolean enabled,
                      @Param("builtin") Boolean builtin);

    /**
     * 按配置键取单行（全局默认行优先于租户行：`tenant_id` 升序 → 0 在前）
     *
     * <p>注意：{@code sys_config} 的唯一约束是 {@code UNIQUE(param_key)}（不含 tenant_id），
     * 所以同一键**只会有一行**；此处的排序只是为将来放开「租户覆盖」时的兼容写法。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
            + "SELECT " + COLS + " FROM sys_config WHERE deleted = 0 AND param_key = #{configKey} "
            + "<if test='tenantId != null'> AND tenant_id IN (0, #{tenantId}) </if>"
            + "ORDER BY tenant_id ASC, id ASC LIMIT 1"
            + "</script>")
    SystemConfig selectByKey(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

    /** 按主键取单行（跨租户可见，供「内置配置不可删」判定与详情回读） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT " + COLS + " FROM sys_config WHERE deleted = 0 AND id = #{id}")
    SystemConfig selectByIdAnyTenant(@Param("id") Long id);

    /** 取主键序列的下一个值（`sys_config.id` 的默认值就是该序列，显式取号避免依赖驱动回填） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT nextval('sys_config_id_seq')")
    Long selectNextId();

    /**
     * 统计「商品是否引用了某个不可逆配置」——`locked`（被商品启用后不能更改）的判定依据。
     *
     * <p>真实链路（详见《系统参数开发文档》§8.4 与《商品开发文档》）：商品档案上有 3 个
     * 单商品粒度开关，加上商品规格属性定义表，正好对应「行业设置」里 4 个不可逆配置项：</p>
     * <ul>
     *   <li>{@code erp_product.is_batch_expiry_managed = 1}（商品表单「保质期/批次号」勾选框）
     *       → {@code industry.batch.shelfLife}（保质期管理）</li>
     *   <li>{@code erp_product.is_batch_managed = 1} → {@code industry.batch.batchNo}（批号管理）
     *       —— 该列在商品实体里存在但商品表单尚未暴露，故目前恒为 0</li>
     *   <li>{@code erp_product.is_serial_managed = 1} → {@code industry.serial.enabled}（序列号管理）
     *       —— 同上</li>
     *   <li>{@code erp_product_attribute_def} 存在行（商品已建规格属性）
     *       → {@code industry.product.specAttr}（商品规格属性）</li>
     * </ul>
     *
     * <p>口径：故意 {@code @InterceptorIgnore(tenantLine)} 且**不加** {@code tenant_id} 条件 ——
     * 这 4 个配置项是 {@code tenant_id = 0} 的全局行，只要系统里**任一**商品启用了它，
     * 全局配置就不可再改（与 ql361 的语义一致）。</p>
     *
     * @return 键为引用类型（batchExpiry/batchNo/serial/specAttr）、值为引用行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT "
            + "(SELECT COUNT(*) FROM erp_product WHERE deleted = 0 AND is_batch_expiry_managed = 1) AS \"batchExpiry\", "
            + "(SELECT COUNT(*) FROM erp_product WHERE deleted = 0 AND is_batch_managed = 1) AS \"batchNo\", "
            + "(SELECT COUNT(*) FROM erp_product WHERE deleted = 0 AND is_serial_managed = 1) AS \"serial\", "
            + "(SELECT COUNT(*) FROM erp_product_attribute_def WHERE deleted = 0) AS \"specAttr\"")
    Map<String, Object> countProductReferences();

    /** 新增参数行（显式给定主键，调用方先 {@link #selectNextId()} 取号） */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO sys_config "
            + "(id, param_key, param_name, param_value, config_type, config_group, nav_group, parent_key, "
            + " value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, "
            + " create_time, update_time, deleted) VALUES ("
            + "#{c.id}, #{c.configKey}, #{c.configName}, #{c.configValue}, #{c.configType}, #{c.configGroup}, "
            + "#{c.navGroup}, #{c.parentKey}, #{c.valueType}, COALESCE(#{c.systemConfig}, false), "
            + "COALESCE(#{c.description}, ''), #{c.helpText}, #{c.tipText}, COALESCE(#{c.sortOrder}, 0), "
            + "COALESCE(#{c.enabled}, true), COALESCE(#{c.locked}, false), COALESCE(#{c.tenantId}, 0), "
            + "NOW(), NOW(), 0)")
    int insertConfig(@Param("c") SystemConfig config);

    /**
     * 整行更新（按主键）
     *
     * <p>用**全列覆盖**而非 {@code updateById}：一来 {@code updateById} 会忽略 null 字段
     * （清空字段必须 {@code UpdateWrapper.set(null)}），二来多租户插件会给它追加
     * {@code tenant_id = <会话租户>} → 改不动 {@code tenant_id = 0} 的全局默认行。
     * 调用方必须先读后写（read-modify-write），保证传入的是完整行。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE sys_config SET "
            + "param_name = #{c.configName}, param_value = #{c.configValue}, config_type = #{c.configType}, "
            + "config_group = #{c.configGroup}, nav_group = #{c.navGroup}, parent_key = #{c.parentKey}, "
            + "value_type = #{c.valueType}, remark = COALESCE(#{c.description}, ''), help_text = #{c.helpText}, "
            + "tip_text = #{c.tipText}, sort_order = COALESCE(#{c.sortOrder}, 0), "
            + "enabled = COALESCE(#{c.enabled}, true), locked = COALESCE(#{c.locked}, false), "
            + "update_time = NOW() "
            + "WHERE deleted = 0 AND id = #{c.id}")
    int updateConfig(@Param("c") SystemConfig config);

    /** 按主键逻辑删除（返回实际影响行数，供「是否真删掉了」判定） */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE sys_config SET deleted = 1, update_time = NOW() WHERE deleted = 0 AND id = #{id}")
    int logicDeleteById(@Param("id") Long id);

    /** 按主键批量逻辑删除（真批量，非循环单删） */
    @InterceptorIgnore(tenantLine = "true")
    @Update("<script>"
            + "UPDATE sys_config SET deleted = 1, update_time = NOW() WHERE deleted = 0 AND id IN "
            + "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>"
            + "</script>")
    int logicDeleteByIds(@Param("ids") List<Long> ids);
}
