package cn.aiedge.base.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 「配置按租户隔离」的读写测试（2026-09-21 专项）。
 *
 * <p>要防的是 {@code SysConfigServiceImpl} 里曾经那句
 * {@code private static final Long CURRENT_TENANT_ID = 1L; // 简化实现}：
 * 它让**所有租户**的配置读写都落在租户 1 身上，于是
 * 「租户 2 读到的库存管理模式/费用审批人/商城排序全是租户 1 的」，
 * 且「租户 2 一保存就把租户 1 的配置覆盖掉」。</p>
 *
 * <p>本测试钉死四条口径：
 * ① 读/写的租户 = **会话租户**（不是常量 1）；
 * ② 本租户没配过 → 回落**平台行 tenant_id = 0**（不是回落到租户 1）；
 * ③ 写路径**严格本租户**取"已存在行"（绝不把平台行当自己的去更新）；
 * ④ 缓存与历史键都带租户维度（否则多租户下直接串值）。</p>
 */
class SysConfigServiceImplTest {

    private static final Long PLATFORM = 0L;
    private static final String KEY = "inventory.mode";

    private SysProjectConfigMapper configMapper;
    private StringRedisTemplate redisTemplate;
    private ListOperations<String, String> listOps;
    private SysConfigServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // LambdaUpdateWrapper 需要实体的 TableInfo 缓存（运行时由 MyBatis-Plus 扫描建立，
        // 纯单元测试里没有）。不初始化的话，一调 getSqlSegment() 就报
        // 「can not find lambda cache for this entity」。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysProjectConfig.class);

        configMapper = mock(SysProjectConfigMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range(any(), anyLong(), anyLong())).thenReturn(List.of());

        service = new SysConfigServiceImpl(configMapper, redisTemplate);
    }

    @AfterEach
    void tearDown() {
        // 临时租户是 ThreadLocal：不清会渗到下一个用例
        MyBatisPlusConfig.clearTempTenantId();
    }

    private static SysProjectConfig row(Long tenantId, String key, String value) {
        SysProjectConfig row = new SysProjectConfig();
        row.setId(777L);
        row.setTenantId(tenantId);
        row.setConfigKey(key);
        row.setConfigValue(value);
        return row;
    }

    // ══════════════════ 读路径 ══════════════════

    @Test
    @DisplayName("读：查询条件里的租户 = 会话租户（不是常量 1）")
    void readsBySessionTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(2L, KEY)).thenReturn("SERIAL");

        assertEquals("SERIAL", service.getValue(KEY, null));

        verify(configMapper).getConfigValue(2L, KEY);
        verify(configMapper, never()).getConfigValue(1L, KEY);
    }

    @Test
    @DisplayName("本租户没配过 → 回落平台行 tenant_id=0（**不是**租户 1）")
    void fallsBackToPlatformRowNotTenantOne() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(2L, KEY)).thenReturn(null);
        when(configMapper.getConfigValue(PLATFORM, KEY)).thenReturn("BATCH");

        assertEquals("BATCH", service.getValue(KEY, null));

        verify(configMapper).getConfigValue(PLATFORM, KEY);
        verify(configMapper, never()).getConfigValue(1L, KEY);
    }

    @Test
    @DisplayName("解析不到租户 → 读平台行，绝不猜一个具体租户")
    void noSessionReadsPlatformRow() {
        // 不设临时租户、无 Sa-Token 上下文 ⇒ getCurrentTenantIdValue() 返回 null
        when(configMapper.getConfigValue(PLATFORM, KEY)).thenReturn("SKU");

        assertEquals("SKU", service.getValue(KEY, null));

        verify(configMapper).getConfigValue(PLATFORM, KEY);
        verify(configMapper, never()).getConfigValue(1L, KEY);
    }

    @Test
    @DisplayName("缓存按租户隔离：租户 2 缓存过的键，租户 3 读不到它的值")
    void cacheIsTenantScoped() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(2L, KEY)).thenReturn("A");
        assertEquals("A", service.getValue(KEY, null));

        // 切到租户 3：若缓存仍只有「配置键」一个维度，这里会命中租户 2 缓存下来的 "A"
        MyBatisPlusConfig.setTempTenantId(3L);
        when(configMapper.getConfigValue(3L, KEY)).thenReturn("B");
        assertEquals("B", service.getValue(KEY, null), "租户 3 不得读到租户 2 缓存的值");

        verify(configMapper).getConfigValue(3L, KEY);
    }

    // ══════════════════ 写路径 ══════════════════

    @Test
    @DisplayName("写：新增行落在会话租户上（且显式补 deleted=0）")
    void insertGoesToSessionTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(null);

        service.setValue(KEY, "SERIAL", "string", "inventory", "库存管理模式");

        ArgumentCaptor<SysProjectConfig> captor = ArgumentCaptor.forClass(SysProjectConfig.class);
        verify(configMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getTenantId(), "新行必须落在会话租户上");
        assertEquals(0, captor.getValue().getDeleted(), "deleted 列 NOT NULL，必须显式给 0");
        verify(configMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("写：本租户没配过时新增，**不去更新平台行**")
    void neverUpdatesPlatformRow() {
        MyBatisPlusConfig.setTempTenantId(2L);
        // 平台行确实存在，但本租户没有 —— 原实现会因为"回落查到了平台行"而更新它
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(null);

        service.setValue(KEY, "SERIAL");

        verify(configMapper).insert(any(SysProjectConfig.class));
        verify(configMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("写：本租户已有行 → 更新的是那一行的 id，租户不变")
    void updatesOwnRowById() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(row(2L, KEY, "BATCH"));

        service.setValue(KEY, "SERIAL");

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.AbstractWrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.AbstractWrapper.class);
        verify(configMapper).update(isNull(), captor.capture());
        // MyBatis-Plus 的参数值是懒写入的：先生成一次 SQL 片段才会填进 paramNameValuePairs
        captor.getValue().getSqlSegment();
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(777L),
                "更新条件必须落在本租户那一行的 id 上");

        verify(configMapper).selectRowByTenant(2L, KEY);
        verify(configMapper, never()).insert(any(SysProjectConfig.class));
    }

    @Test
    @DisplayName("删：严格本租户查行（否则会把平台行/别的租户的行逻辑删除掉）")
    void deleteUsesOwnTenantRow() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(row(2L, KEY, "BATCH"));

        service.deleteConfig(KEY);

        verify(configMapper).selectRowByTenant(2L, KEY);
        verify(configMapper, never()).selectRowByTenant(eq(PLATFORM), any());
        verify(configMapper).update(isNull(), any());
    }

    // ══════════════════ 列表 / 刷新 / 历史 ══════════════════

    @Test
    @DisplayName("列表与刷新都只针对会话租户")
    void listAndRefreshScopedToTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectAllConfigs(2L)).thenReturn(List.of(row(2L, KEY, "BATCH")));

        assertEquals(1, service.getAllConfigs().size());
        service.refreshCache();

        verify(configMapper, times(2)).selectAllConfigs(2L);
        verify(configMapper, never()).selectAllConfigs(1L);
    }

    @Test
    @DisplayName("刷新单个键用与读取相同的回落口径（本租户 → 平台行）")
    void refreshConfigKeepsFallbackSemantics() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(2L, KEY)).thenReturn(null);
        when(configMapper.getConfigValue(PLATFORM, KEY)).thenReturn("BATCH");

        service.refreshConfig(KEY);

        // 刷新后再读：应拿到回落平台行的值，而不是"刷新把值刷没了"
        assertEquals("BATCH", service.getValue(KEY, null));
        verify(configMapper, never()).getConfigValue(1L, KEY);
    }

    @Test
    @DisplayName("配置历史的 Redis 键含租户（否则能翻到/回滚成别的租户的旧值）")
    void historyKeyIncludesTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);

        service.getConfigHistory(KEY);

        verify(listOps).range("sys:config:history:2:" + KEY, 0, 99);
    }

    @Test
    @DisplayName("变更事件带 tenantId（订阅方才能按租户落缓存）")
    void publishedMessageCarriesTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(null);

        service.setValue(KEY, "SERIAL");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("sys:config:change"), captor.capture());
        assertTrue(captor.getValue().contains("\"tenantId\":\"2\""),
                "事件里必须带租户；实际=" + captor.getValue());
    }

    @Test
    @DisplayName("无会话写入 → 落平台行（0），不落某个具体租户")
    void noSessionWriteGoesToPlatform() {
        when(configMapper.selectRowByTenant(PLATFORM, KEY)).thenReturn(null);

        service.setValue(KEY, "SERIAL");

        ArgumentCaptor<SysProjectConfig> captor = ArgumentCaptor.forClass(SysProjectConfig.class);
        verify(configMapper).insert(captor.capture());
        assertEquals(PLATFORM, captor.getValue().getTenantId());
    }

    @Test
    @DisplayName("getConfig 严格本租户：没配过就是 empty，不显示平台行")
    void getConfigIsStrictlyOwnTenant() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.selectRowByTenant(2L, KEY)).thenReturn(null);

        assertTrue(service.getConfig(KEY).isEmpty());
        // 回落只发生在"读生效值"那条线上；管理视图不该出现别人的行
        verify(configMapper, never()).getConfigValue(anyLong(), any());
        verify(configMapper, never()).getConfigValue(eq(PLATFORM), any());
    }

    @Test
    @DisplayName("空/仅平台行的库也照常工作：查不到就返回默认值")
    void missingEverythingReturnsDefault() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(anyLong(), any())).thenReturn(null);

        assertEquals("BATCH", service.getValue(KEY, "BATCH"));
        assertTrue(service.getValue(KEY).isEmpty());
    }

    @Test
    @DisplayName("intValue 便捷方法走同一条租户口径")
    void typedGetterUsesSameScope() {
        MyBatisPlusConfig.setTempTenantId(2L);
        when(configMapper.getConfigValue(2L, "marketing.autoCampaign.globalFreqCap")).thenReturn("5");

        assertEquals(5, service.getIntValue("marketing.autoCampaign.globalFreqCap", 2));
        verify(configMapper, never()).getConfigValue(1L, "marketing.autoCampaign.globalFreqCap");
    }
}
