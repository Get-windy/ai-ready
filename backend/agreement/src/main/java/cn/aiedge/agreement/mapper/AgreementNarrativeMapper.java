package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementNarrative;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文字版（{@code agreement_narrative}）Mapper。
 *
 * <p>系统级表（{@code tenant_id} 恒为 0）、协议天然跨租户 ⇒ 同 {@link AgreementSettingMapper}，
 * 用 Mapper 级 {@code @InterceptorIgnore} 关掉租户注入，可见性一律走 {@code AgreementVisibility}。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementNarrativeMapper extends BaseMapper<AgreementNarrative> {
}
