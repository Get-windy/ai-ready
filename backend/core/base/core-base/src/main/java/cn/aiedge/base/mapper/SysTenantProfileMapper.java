package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysTenantProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户企业档案 Mapper（1:1 于 {@link cn.aiedge.base.entity.SysTenant}）
 *
 * <p>表 {@code sys_tenant_profile}（迁移 V11.420.0）；由 {@code @MapperScan("cn.aiedge.**.mapper")}
 * 自动注册。本表**有** {@code tenant_id} 且不在忽略清单中 → 常规查询由多租户插件自动收敛到会话租户，
 * 服务层仍会显式带租户条件（口径双保险，见 {@code TenantProfileService}）。</p>
 */
@Mapper
public interface SysTenantProfileMapper extends BaseMapper<SysTenantProfile> {
}
