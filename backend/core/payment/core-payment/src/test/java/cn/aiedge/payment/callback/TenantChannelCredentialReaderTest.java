package cn.aiedge.payment.callback;

import cn.aiedge.base.entity.SysProjectConfig;
import cn.aiedge.base.mapper.SysProjectConfigMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 「支付凭据只能本租户自用」的读取侧测试（2026-09-21）。
 *
 * <p>要防的是这条链路上真实存在的问题：
 * {@code cn.aiedge.base.service.impl.SysConfigServiceImpl} 里
 * {@code private static final Long CURRENT_TENANT_ID = 1L;} 是**硬编码「简化实现」**，
 * 它的 {@code getValue} 一律读租户 1 —— 既不认会话租户也不认临时租户。
 * 若用它读支付凭据，则**所有租户的回调都会拿租户 1 的凭据验签**。</p>
 *
 * <p>本测试钉死 {@link TenantChannelCredentialReader} 的两条口径：
 * ① 查询条件里的租户 = **调用方传入的租户**（不是常量 1、不是会话上下文）；
 * ② 租户为 null 时**直接拒绝查询**，绝不猜一个租户出来。</p>
 */
class TenantChannelCredentialReaderTest {

    private static final String KEY = "payment.channel.alipay";

    private SysProjectConfigMapper mapper;
    private TenantChannelCredentialReader reader;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 需要实体的 TableInfo 缓存（运行时由 MyBatis-Plus 扫描建立，
        // 纯单元测试里没有）。不初始化的话，一调 getSqlSegment() 就报
        // 「can not find lambda cache for this entity」。
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysProjectConfig.class);

        mapper = mock(SysProjectConfigMapper.class);
        reader = new TenantChannelCredentialReader(mapper);
    }

    @Test
    @DisplayName("按传入租户读取：查询条件里的 tenant_id 就是传入值（不是常量 1）")
    void readsByGivenTenant() {
        SysProjectConfig row = new SysProjectConfig();
        row.setConfigValue("{\"enabled\":true}");
        when(mapper.selectOne(any())).thenReturn(row);

        String value = reader.read(7L, KEY);
        assertEquals("{\"enabled\":true}", value);

        // 抓取实际传给 Mapper 的查询条件，确认租户是 7 而不是别的
        // （getParamNameValuePairs 在 AbstractWrapper 上，Wrapper 接口没有，故按 AbstractWrapper 抓）
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.AbstractWrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.AbstractWrapper.class);
        verify(mapper).selectOne(captor.capture());
        // ⚠️ MyBatis-Plus 的参数值是**懒写入**的：eq() 只挂了个 lambda，
        // 必须先生成一次 SQL 片段（getSqlSegment）才会填进 paramNameValuePairs。
        // 直接读会得到空表 —— 第一版就是这样把「参数为空」误当成「租户没设」。
        captor.getValue().getSqlSegment();
        Collection<Object> params = captor.getValue().getParamNameValuePairs().values();
        assertTrue(params.contains(7L),
                "查询条件里必须出现传入的租户 7；实际参数=" + params);
        assertFalse(params.contains(1L) && !params.contains(7L),
                "不得退化成读取固定租户 1");
    }

    @Test
    @DisplayName("租户为 null → 直接返回 null 且**不查库**（绝不猜租户）")
    void nullTenantNeverQueries() {
        assertNull(reader.read(null, KEY));
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("配置键为空 → 不查库")
    void blankKeyNeverQueries() {
        assertNull(reader.read(1L, ""));
        assertNull(reader.read(1L, null));
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("查不到行 → 返回 null（调用方据此判 isConfigured=false，渠道不生效）")
    void missingRowReturnsNull() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertNull(reader.read(2L, KEY));
    }

    @Test
    @DisplayName("查询异常 → 返回 null 而不是抛出（回调链路不应因配置读取炸掉）")
    void queryFailureReturnsNull() {
        when(mapper.selectOne(any())).thenThrow(new RuntimeException("db down"));
        assertNull(reader.read(3L, KEY));
    }
}
