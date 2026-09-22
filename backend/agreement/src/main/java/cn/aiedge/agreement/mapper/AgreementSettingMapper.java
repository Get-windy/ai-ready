package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementSetting;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段设定版（值）Mapper。
 *
 * <p><b>⚠️ 为什么必须 {@code @InterceptorIgnore(tenantLine = "true")}</b>：
 * 本表与协议主档同为**系统级**（{@code tenant_id} 恒为 0），而协议天然跨租户 ——
 * 若让租户拦截器按会话租户注入 {@code tenant_id = <会话租户>}，那么
 * 另一端租户（乙方）**一行设定也读不到**，等于协议内容只有一方看得见，模块白做。
 * 这条与裁定⑥对 {@code agreement} 四张表的处置同因同果；
 * 因 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 属 core-base（不在本模块可改范围），
 * 这里改用 MyBatis-Plus 的**Mapper 级**忽略开关，效果等价：本 Mapper 的任何方法都不注入租户条件。</p>
 *
 * <p>由此产生的纪律：本表上的跨租户读写必须**自己写对**条件，且
 * "这份协议能不能看"一律走 {@code AgreementVisibility}（唯一构造处），
 * 不许在本模块别处手写 {@code party_a_tenant_id = ? OR party_b_tenant_id = ?}。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementSettingMapper extends BaseMapper<AgreementSetting> {
}
