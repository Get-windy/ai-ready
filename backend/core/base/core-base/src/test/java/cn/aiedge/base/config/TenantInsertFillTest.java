package cn.aiedge.base.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@code MetaObjectHandler.insertFill} 的租户填充口径（平台-BREAK-02）。
 *
 * <p><b>要防的是什么</b>：原实现是
 * {@code Long tenantId = getCurrentTenantIdValue(); if (tenantId != null) metaObject.setValue("tenantId", tenantId);}
 * —— 只要会话里有租户就**无条件覆盖**实体上的 tenantId。后果是「调用方明确指定租户的写入
 * 落到会话租户头上」，即跨租户写错位。项目自己已踩过两次：
 * {@code SysTenantMenuMapper}（给租户 A 授权写到会话租户，2026-09-18）与
 * {@code sys_role_permission} 里 16 行租户标记不一致。</p>
 *
 * <p><b>钉死的三条口径</b>：
 * ① 实体已显式指定 tenantId ⇒ 原样保留，绝不被会话租户覆盖；
 * ② 实体未指定且会话有租户 ⇒ 按会话租户填充；
 * ③ 实体未指定且会话无租户 ⇒ 保持 null（不瞎填，由拦截器/DB 约束兜底）。</p>
 *
 * <p>用 {@link MyBatisPlusConfig#setTempTenantId} 造会话租户：它是拦截器读租户的**第一优先级**来源
 * （见 {@code getCurrentTenantIdValue}），因此本测试无需起 Spring 上下文与 Sa-Token 会话。</p>
 *
 * @author AI-Ready Team
 */
@DisplayName("租户字段自动填充（平台-BREAK-02）")
class TenantInsertFillTest {

    /** 会话租户 */
    private static final Long SESSION_TENANT = 9L;
    /** 调用方显式指定的另一个租户 */
    private static final Long EXPLICIT_TENANT = 7L;

    private MetaObjectHandler handler;

    @BeforeEach
    void setUp() {
        // strictInsertFill 依赖实体的 TableInfo 缓存（运行时由 MyBatis-Plus 扫描建立，纯单测里没有），
        // 不初始化会直接抛 IllegalStateException("tableInfo can't be null")
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), FillProbe.class);

        handler = new MyBatisPlusConfig().metaObjectHandler();
    }

    @AfterEach
    void tearDown() {
        // 临时租户是 ThreadLocal，不清会渗到下一个用例
        MyBatisPlusConfig.clearTempTenantId();
    }

    @Test
    @DisplayName("已显式指定租户 ⇒ 保留原值，不被会话租户覆盖")
    void keepsExplicitTenant() {
        MyBatisPlusConfig.setTempTenantId(SESSION_TENANT);
        FillProbe probe = new FillProbe();
        probe.setTenantId(EXPLICIT_TENANT);

        handler.insertFill(metaObjectOf(probe));

        // 修复前这里是 9（会话租户），正是「给租户 A 建数据却落到会话租户头上」
        assertEquals(EXPLICIT_TENANT, probe.getTenantId(),
                "显式指定的 tenantId 被自动填充覆盖了 —— 跨租户写错位");
    }

    @Test
    @DisplayName("未指定租户且有会话租户 ⇒ 按会话租户填充")
    void fillsFromSessionTenantWhenAbsent() {
        MyBatisPlusConfig.setTempTenantId(SESSION_TENANT);
        FillProbe probe = new FillProbe();

        handler.insertFill(metaObjectOf(probe));

        assertEquals(SESSION_TENANT, probe.getTenantId());
    }

    @Test
    @DisplayName("未指定租户且无会话租户 ⇒ 保持 null，不臆造租户")
    void leavesNullWhenNoTenantContext() {
        FillProbe probe = new FillProbe();

        handler.insertFill(metaObjectOf(probe));

        assertNull(probe.getTenantId());
    }

    @Test
    @DisplayName("显式指定租户不影响其它公共字段的正常填充")
    void stillFillsOtherCommonFields() {
        MyBatisPlusConfig.setTempTenantId(SESSION_TENANT);
        FillProbe probe = new FillProbe();
        probe.setTenantId(EXPLICIT_TENANT);

        handler.insertFill(metaObjectOf(probe));

        assertNotNull(probe.getCreateTime(), "createTime 应被填充");
        assertEquals(0, probe.getVersion(), "version 应被填为初始值 0");
        assertEquals(EXPLICIT_TENANT, probe.getTenantId());
    }

    private static MetaObject metaObjectOf(FillProbe probe) {
        return SystemMetaObject.forObject(probe);
    }

    /**
     * 最小探针实体：字段与填充注解照抄 {@code cn.aiedge.base.entity.BaseEntity} 的相关部分，
     * 但**不继承它**，以免把无关字段（逻辑删除、乐观锁语义）牵进断言。
     * setter 一律用传统 void 写法，避免链式 setter 在 MyBatis 反射层上的行为差异。
     */
    @TableName("tenant_fill_probe")
    static class FillProbe {

        @TableId(type = IdType.ASSIGN_ID)
        private Long id;

        private Long tenantId;

        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(fill = FieldFill.INSERT)
        private Integer version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }
}
