package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 履约方式集合 Mapper。
 *
 * <p>系统级表（{@code tenant_id} 恒为 0）、协议天然跨租户 ⇒ 同 {@link AgreementSettingMapper}，
 * 用 Mapper 级 {@code @InterceptorIgnore} 关掉租户注入（否则订单路由作为"第三方链路"
 * 去读这份协议时会被注入会话租户条件，读不到对方那端约定的履约方式）。</p>
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface AgreementFulfillmentModeMapper extends BaseMapper<AgreementFulfillmentMode> {
}
