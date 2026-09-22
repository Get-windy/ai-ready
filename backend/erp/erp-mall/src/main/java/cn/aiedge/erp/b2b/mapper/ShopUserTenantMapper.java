package cn.aiedge.erp.b2b.mapper;

import cn.aiedge.erp.b2b.model.ShopUserTenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 「系统顾客 × 租户」关联 Mapper。
 *
 * <p>注意本表**未**登记进 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}：
 * 它本身就是租户维度数据，常规查询让租户拦截器按会话租户过滤是正确的。
 * 因此下面那些手写 SQL 里的 {@code tenant_id} 条件是**给拦截器兜底/显式化**用的，
 * 与自动注入的条件一致、不会互相冲突。</p>
 */
@Mapper
public interface ShopUserTenantMapper extends BaseMapper<ShopUserTenant> {

    /** 某系统顾客在某租户下的关联（用于登录校验与注册去重） */
    @Select("SELECT * FROM shop_user_tenant WHERE shop_user_id = #{shopUserId} "
            + "AND tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    ShopUserTenant selectLink(@Param("shopUserId") Long shopUserId, @Param("tenantId") Long tenantId);

    /** 某租户下全部关联（后台顾客管理；按状态可选） */
    @Select("<script>SELECT * FROM shop_user_tenant WHERE tenant_id = #{tenantId} AND deleted = 0 "
            + "<if test='status != null'> AND status = #{status} </if> "
            + "ORDER BY create_time DESC</script>")
    List<ShopUserTenant> selectByTenant(@Param("tenantId") Long tenantId,
                                        @Param("status") Integer status);
}
